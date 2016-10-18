define(function(require, exports, module) {

    var _ = require('underscore');
    var SimpleSplunkView = require("splunkjs/mvc/simplesplunkview");  
    var nester = require("../underscore-nest/underscore-nest");
    var d3 = require("../d3/d3");

    require("css!./sunburst.css");

    window.nester = nester;

    var Sunburst = SimpleSplunkView.extend({
        moduleId: module.id,

        className: "splunk-toolkit-sunburst", 

        options: {
            managerid: null,  
            data: 'preview', 
            chartTitle: null,
            valueField: null,
            categoryFields: null,
            formatLabel: _.identity,
            formatTooltip: function(d) {
                return (d.name || "Total") + ": " + d.value;
            }
        },

        output_mode: "json_rows",

        initialize: function() {
            SimpleSplunkView.prototype.initialize.apply(this, arguments);

            // TODO: enable push
            // TODO: wire up changes

            this.settings.on("change:valueField", this.render, this);
            this.settings.on("change:categoryFields", this.render, this);

            // Set up resize callback. The first argument is a this
            // pointer which gets passed into the callback event
            $(window).resize(this, _.debounce(this._handleResize, 20));
        },

        _handleResize: function(e){
            
            // e.data is the this pointer passed to the callback.
            // here it refers to this object and we call render()
            e.data.render();
        },

        createView: function() {
            // Here we wet up the initial view layout
            var margin = {top: 30, right: 30, bottom: 30, left: 30};
            var availableWidth = parseInt(this.settings.get("width") || this.$el.width());
            var availableHeight = parseInt(this.settings.get("height") || this.$el.height());

            this.$el.html("");

            var svg = d3.select(this.el)
                .append("svg")
                .attr("width", availableWidth)
                .attr("height", availableHeight)
                .attr("pointer-events", "all");

            // The returned object gets passed to updateView as viz
            return { container: this.$el, svg: svg, margin: margin};
        },

        // making the data look how we want it to for updateView to do its job
        formatData: function(data) {
            var valueField = this.settings.get('valueField');
            var rawFields = this.resultsModel.data().fields;
            var fieldList = this.settings.get("categoryFields");
            if(fieldList){
                fieldList = fieldList.split(/[ ,]+/);
            }
            else{
                fieldList = this.resultsModel.data().fields;
            }
            var objects = _.map(data, function(row) {
                return _.object(rawFields, row);
            });
            var dataResults = nester.nest(objects, fieldList, function(children) {
                var total = 0;
                _.each(children, function(child){
                    var size = child[valueField] || 1;
                    total += size;
                })
                return total;
            });
            dataResults['name'] = this.settings.get("chartTitle") || "";
            data = {
                'results': dataResults,
                'fields': fieldList
            }
            return data;
        },

        updateView: function(viz, data) {
            var that = this;
            var formatLabel = this.settings.get("formatLabel") || _.identity;
            var formatTooltip = this.settings.get("formatTooltip") || function(d) { return d.name; };
            var containerHeight = this.$el.height();
            var containerWidth = this.$el.width(); 

            // Clear svg
            var svg = $(viz.svg[0]);
            svg.empty();
            svg.height(containerHeight);
            svg.width(containerWidth);

            // Add the graph group as a child of the main svg
            var graphWidth = containerWidth - viz.margin.left - viz.margin.right
            var graphHeight = containerHeight - viz.margin.top - viz.margin.bottom;
            var graph = viz.svg
                .append("g")
                .attr("width", graphWidth)
                .attr("height", graphHeight)
                .attr("transform", "translate("  
                        + ((graphWidth/2) + viz.margin.left ) + ","  
                        + ((graphHeight/2) + viz.margin.top ) + ")");

            var radius = Math.min(graphWidth, graphHeight) / 2;
            
            var color = d3.scale.category20c();

            var x = d3.scale.linear()
                .range([0, 2 * Math.PI]);

            var y = d3.scale.linear()
                .range([0, radius]);

            var partition = d3.layout.partition()
                .value(function(d) { return d['value']; });

            var arc = d3.svg.arc()
                .startAngle(function(d) { return Math.max(0, Math.min(2 * Math.PI, x(d.x))); })
                .endAngle(function(d) { return Math.max(0, Math.min(2 * Math.PI, x(d.x + d.dx))); })
                .innerRadius(function(d) { return Math.max(0, y(d.y)); })
                .outerRadius(function(d) { return Math.max(0, y(d.y + d.dy)); });

            var root = data.results;

            var g = graph.selectAll("g")
                .data(partition.nodes(root))
                .enter().append("g");

            var path = g.append("path")
                .attr("d", arc)
                .style("fill", function(d) {return color((d.children ? d : d.parent).name); })
                .on("click", click);
                
            path.append("title")
                .text(formatTooltip);

            var text = g.append("text")
                .attr("text-anchor", function(d) {
                 return x(d.x + d.dx / 2) > Math.PI ? "end" : "start";
                })
                .attr("transform", function(d) {
                    var angle = x(d.x + d.dx / 2) * 180 / Math.PI - 90;
                    var rotate = angle;
                    var padding = 5;
                    return "rotate(" + rotate + ")translate(" + (y(d.y) + padding) + ")rotate(" + (angle > 90 ? -180 : 0) + ")";
                })
                .attr("dy", ".2em")
                .attr("x", 0)
                .text(function(d) { return formatLabel(d.name); })
                .on("click", click);
                
            text.append("title")
                .text(formatTooltip);

            function click(d) {
            // fade out all text elements
                text.transition().attr("opacity", 0);

                path.transition()
                  .duration(750)
                  .attrTween("d", arcTween(d))
                  .each("end", function(e, i) {
                      // check if the animated element's data e lies within the visible angle span given in d
                      if (e.x >= d.x && e.x < (d.x + d.dx)) {
                        // get a selection of the associated text element
                        var arcText = d3.select(this.parentNode).select("text");
                        // fade in the text element and recalculate positions
                        arcText.transition().duration(750)
                            .attr("opacity", 1)
                            .attr("text-anchor", function(d) {
                             return x(d.x + d.dx / 2) > Math.PI ? "end" : "start";
                            })
                            .attr("transform", function(d) {
                                var angle = x(d.x + d.dx / 2) * 180 / Math.PI - 90;
                                var rotate = angle;
                                var padding = 5;
                                return "rotate(" + rotate + ")translate(" + (y(d.y) + padding) + ")rotate(" + (angle > 90 ? -180 : 0) + ")";
                            })
                            .attr("dy", ".2em")
                            .attr("x", 0)
                      }
                  });
            }

            // Interpolate the scales!
            function arcTween(d) {
              var xd = d3.interpolate(x.domain(), [d.x, d.x + d.dx]),
                  yd = d3.interpolate(y.domain(), [d.y, 1]),
                  yr = d3.interpolate(y.range(), [d.y ? 20 : 0, radius]);
              return function(d, i) {
                return i
                    ? function(t) { return arc(d); }
                    : function(t) { x.domain(xd(t)); y.domain(yd(t)).range(yr(t)); return arc(d); };
              };
            }

            function computeTextRotation(d) {
              return (x(d.x + d.dx / 2) - Math.PI / 2) / Math.PI * 180;
            }
                    
        }
    });
    return Sunburst;
});