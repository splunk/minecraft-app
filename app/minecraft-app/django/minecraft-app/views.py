from django.contrib.auth.decorators import login_required
from splunkdj.decorators.render import render_to

@render_to('minecraft-app:home.html')
@login_required
def home(request):
    return {
        "message": "Hello World from minecraft-app!",
        "app_name": "minecraft-app",
        "app_label": "Splunk for Minecraft"
    }

@render_to()
@login_required
def render_page(request, tmpl="minecraft-app:home.html"):
	return {
		"TEMPLATE": "minecraft-app:%s.html" % tmpl,
		"app_label": "Splunk for Minecraft"
	}             
