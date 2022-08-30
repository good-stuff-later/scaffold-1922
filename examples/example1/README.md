# DbLeaderElection - Example 1

This is a plain Java SE example which demonstrates the use of the DbLeaderElection library.


The application exposes a simple HTTP server which allows the user to
- Force the instance to relinquish leadership
- Exit the application


### Usage




### Requirements

PostgreSQL database which must hava a database named `leaderelect` and a table
for leader election already created. See library documentation for the name and 
structure of this table.