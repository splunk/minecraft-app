from django.conf import settings
from django.utils import importlib
from django.core.urlresolvers import reverse, resolve

NAV = [
	{
		"name": "SplunkCraft",
		"link": reverse("splunk-demo-minecraft:home") # TODO don't hardcode the app name
	}

]