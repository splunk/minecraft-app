from django.contrib.auth.decorators import login_required
from appfx.decorators.render import render_to

@render_to('splunk-demo-minecraft:home.html')
@login_required
def home(request):
    return {
        "message": "Hello World from splunk-demo-minecraft!",
        "app_name": "splunk-demo-minecraft",
        "app_label": "Splunk for Minecraft"
    }

@render_to()
@login_required
def render_page(request, tmpl="splunk-demo-minecraft:home.html"):
	return {
		"TEMPLATE": "splunk-demo-minecraft:%s.html" % tmpl,
		"app_label": "Splunk for Minecraft"
	}             
