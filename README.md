This is a fork of the original [Build Token Root Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Build+Token+Root+Plugin)
that allows me to get the HTTP content data when the URL is triggered by a POST request.

The added code is intended to allow me to access the data that Github and BitBucket send in the body.

The change adds a StringParameterValue named `payload` whose content is straight from the request input stream.

I hit the problem when I was trying to follow a [blog article](http://chloky.com/github-json-payload-in-jenkins/)
about processing hooks in Jenkins, but it didn't seem to be working when used with the Build Token Root Plugin.

# Installing #
I haven't published this plugin anywhere, in order to use it:
* clone the code
* install maven
* mvn package (or whatever, I used my IDE)
* upload the hpi file from the target directory to Jenkins

# SECURITY WARNING #
Given that the whole point of this plugin is to allow anonymous access to the URL, you would want to be very careful
about the build task you're linking to this URL.  Don't do anything dumb like passing the content of any of this data
to the command line.
Thinking about it, could there even be an issue with Jenkins setting the parameter content as an environment variable
(buffer overflow maybe?)  Maybe this whole change is a bad idea :)



