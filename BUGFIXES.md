
# CODEU CHAT SERVER | BUGFIXES

Here are bugs we encountered while working on the project and their fixes.

## Jar File Compilation
Description: The project would not compile (make.sh would not run to completion) after I attempted to install the MongoDB Java Driver to enable database persistence.

Summary: The mongo driver’s jar file was not properly included in the project’s classpath and so was never compiled properly, making java unable to recognize the new packages.

Fix: I edited make.sh so that the new jar file would be included along with the two existing jar files (junit and hamcrest) in compilation. I at first only included the new jar for the src folder but this approach failed to work - it turns out that the tests also used model.java, where the changes were made, but I didn’t make the connection at first since we didn’t write any new tests for the mongo features. Adding the new jar file for compilation for both directories solved the problem.
 
## GUI doesn’t work
Description: GUI starts but is unresponsive. Terminal reports that an error occurred on the server but no error message shows up in the server log.

Summary: run_simple_gui_client.sh was not updated with the rest of the sh files after the main project was updated to take in port numbers as parameters. Thus, the file still contained a hard-coded port number that did not match the server’s port number. The code then threw errors to indicate that some server operations failed but those error messages never showed (because the requested server didn’t exist).

Fix: We modified run_simple_gui_client.sh to take in a port number as a parameter so that the proper port number (the server’s) could be inputted by the user. The gui returned to normal functionality when the port numbers matched.
 
## Order of commands
Description: This was a minor bug where the command “m-list-all” does not show all messages for the current conversation until the currently logged in user adds a message to that conversation and then enters “m-list-all” again. A similar bug was being unable to sign-in until entering “u-list-all” to see all users.

Summary: In ClientMessage.java and ClientUser.java, there are functions used specifically for the commands described in the description. By examining the code in ClientMessage.java where a message was added (m-add related) and where all users were shown (u-list-all related), we found that there were update functions that were not in the m-list-all and sign-in functions.

Fix: Adding the necessary update function to those functions fixed the issue.
