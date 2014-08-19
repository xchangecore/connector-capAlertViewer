connector-capAlertViewer
========================

A GWT GUI to submit CAP Alert to the XchangeCore

Dependencies:
connector-base-util
connector-base-async

To build:
Use maven 
1. Build all the dependencies
2. Build capAlertViewer

To run:
1. Copy the resources/contexts/capAlertSubmitter-context to the same directory as the executable jar file.
2. Open the capAlertSubmitter-context with an editor.
3. Look for the "webServiceTemplate" bean, change the "default uri" to the XchangeCore you are using to run this adapter against.
4. Change the "credentials" to a valid username and password that can access the XchangeCore.
3. Open a cygwin or windows and run "java -jar capAdapter.jar"
