from django.conf.urls import patterns, include, url
from splunkdj.utility.views import render_template as render

urlpatterns = patterns('',
	url(r'^home/$', 'splunk-demo-minecraft.views.home', name='home'),
    url(r'^(?P<tmpl>.*)/$', 'splunk-demo-minecraft.views.render_page', name='tmpl_render')
)
