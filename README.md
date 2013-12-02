# robopocalypse

A Clojure app to interact with the IBM IoT hackathon robopocalypse brief

## Usage

lein run will interact with the hack2 implementation at http://m2m.demos.ibm.com/hack/index.html?topicRoot=hack2, see http://ibminternetofthings.tumblr.com/post/68059106887/hackathon-documentation-building-your-iot-defenses for more details on the other instances and the commands that can be used.

The set "ignore" is the list of robot names that should be ignored (such as other bots people have put in), alternatively modify the code to only look for "things" with 1/2 digits as their ids. 
The color that the disabled robots are set to is picked randomly from the list "colors"
The coordinates in the me atom set the initial coordinates that bot will start at.

## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
