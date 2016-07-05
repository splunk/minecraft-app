from django.conf.urls import patterns, include, url
from splunkdj.utility.views import render_template as render

urlpatterns = patterns('',
	url(r'^home/$', 'minecraft-app.views.home', name='home'),
    url(r'^(?P<tmpl>.*)/$', 'minecraft-app.views.render_page', name='tmpl_render')
)
