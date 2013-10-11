from django.conf import settings
from django.utils import importlib
from django.core.urlresolvers import reverse, resolve

NAV = [
	{
		"name": "MineCraft",
		"link": reverse("minecraft-app:home") # TODO don't hardcode the app name
	}

]