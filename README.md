# KIS-Monitoring

KISMON (for "Keep-it-simple Monitoring") is a tool whose goal it is to monitor the liveness and 
responsiveness of application and DB servers and of entire collections or "environments" of such
components that are required to provide a service, e.g. a compound of a DB, a Mock-Server 
(if applicable like for a test environment), a Proxy-server or load-balancer (if applicable) 
and the Web(Application)Server(s) running an application.

![alt Screenshot of KISMON configured for ZHServices](docs/screenshot_KISMON_for_ZHServices.png "Screenshot of KISMON configured for ZHServices")

## Node-Hierarchies and States

Components form a tree and can be organized and grouped in subtrees. The requests used to probe for the 
corresponding element's conditions can be defined on "leaf-nodes" in that tree. 
For each such "leaf node" conditions can be defined what response is to be considered as "OK". If that 
condition is *not* met it is considered as "Failed".

`Intermediate Nodes` summarize the results of their respective children. That "summarization" can be 
configured, i.e. there are several conditions to choose from what combination of children states is to 
be considered as `OK`, `Degraded` or `Failed` for a parent node. 
Such conditions can be "All children must be OK", "At maximum one child is allowed to be not OK" (this 
can be used in redundant setups where ONE failing component is tolerable but degrades the stability), 
"at most one child must be OK" (this condition can be used to verify that exactly one and only one child 
is active at a time, e.g. in failover setups), etc. 
States "bubble up", i.e. on the next hierachy level the next summarization of children states is applied 
etc. up all the way to the `Root Node`.

With such a setup it is easy to spot which environments or which elements of a larger setup work 
and which don't. This may not provide the immediate root cause but it helps greatly at locating 
the sub-component that may be the reason why some application or an environment does not work.

Remark: there may be more than one `Root Node` (not shown here)! Each `Root Node` with all its 
children corresponds to one configuration file (see further down on config files).
KISMON can load multiple configurations and monitor more than one hierachy at once.  

## Requests and Request-Types
Request-types that are supported by KISMON are `REST`- and `SOAP`-requests, `JDBC`-requests 
(i.e. DB queries), `PING` (does ICMP-pings testing reachability and responsiveness of a system) 
and `SSH`, the latter meaning a "command-line" that is executed after logging into the target system using
an SSH connection. 
This command-line can be any shell command line like a single command, a script name or a compound of 
several concatenated (piped) commands.

![alt Screenshot of KISMON configured for ZHServices with opened form](docs/screenshot_KISMON_for_ZHServices_with_opened_form.png "Screenshot of KISMON configured for ZHServices with opened form")

Requests are specified using a "form" which opens at the right when double-clicking on the first part 
(the icon) of the corresponding line in the tree. Double clicking again hides the form again. 
One can move the divider between the left-hand tree view and the right-hand "form" to adjust how 
much screen space is used for which part.
The forms contain a generic section (with name, a description/comment, and a few fields defining if and 
how frequent that request is to be executed and whether it is to be taken into account when calculating 
the parent node status).
Below that section are a couple of "accordeons", i.e. sections that can be opened (expanded) and closed 
(collapsed) describing the "connection details", the "response details", "validation details" and 
"action details". The content of these sections varies depending on the request *type*. 

* The response details of `REST`- and `SOAP`-commands are the resulting responses from the connected 
service.
* The response of a `JDBC`-command is the resulting DB-output, i.e. a table containing the query results 
ordered by column.
* The response of a `PING`-command is the result of a ping-command as executed on a command-line (KISMON 
extracts the response time from that result).
* The response of an `SSH`-command the resulting output of the command executed on the logged-in user's 
shell.

### Nodes view

The main view of KISMON is clearly the nodes view as shown in the screenshots above. 
It shows the status of the last "poll" and the status hierachy is updated after each update 
(I call this the "bubbling up" of states).

The right hand side form allows to inspect details for each node, esp. to inspect the results
from the last response received (or not received in which case one can see the reason and/or 
the error message).

### History data

Results of the requests (i.e. the node name, the timestamp of each request, the resulting status and 
response times) are saved to a DB. 
By default that is a simple in memory DB (H2) but one could also specify a different one (in the 
`application.properties` file) in case one would want to use the data further, e.g. for some statistics).

#### History table view

There is a tabular history view that allows to browse the history data (with filters to narrow down the 
list to specific host(s) and time spans). With that one can inspect whether a system was up and running 
or not or whether it experienced some slow-down during a specific time period, etc.

![alt Screenshot of KISMON configured for ZHServices with history table](docs/screenshot_KISMON_for_ZHServices_with_history_table.png "Screenshot of KISMON configured for ZHServices with history table")

#### History graphic view

There is also a graphical history view whose purpose is to provide a graphical visualization of the 
response times, but that view is still in a very infant state (i.e. sill very experimental and unstable). 
If the filter is not very narrow (yielding large amounts of data to display) the backend calls of this 
view occasionally hang or even crash the application, so this feature is definitely not 
"production ready", yet. I suggest to ignore it for now.

![alt Screenshot of KISMON configured for ZHServices with history graph](docs/screenshot_KISMON_for_ZHServices_with_history_graph.png "Screenshot of KISMON configured for ZHServices with history graph")

## Configuring the application

### Configuration via Forms

The individual request types and parameters (for leaf nodes) and the summarization rules 
(for intermediate nodes) are configured via different forms.

The generic fields at the top of the forms are the same for all node types. These are 
* the *name* of each "node" (which must be unique in the entire tree)
* an arbitrary *comment* or description
* a toggle whether the node is *applicable*. This flag steers, whether the parent includes or ignores 
that node's result for it "summarization". If the toggle is "off", the node can still be executed 
(or even be "active", i.e. sending a request regularly) but the result is displayed only and not 
otherwise considered for the "bubbling up" of results.
This allows e.g. to prevent a subtree to continuously be shown as failed or degraded if it is known that 
some system is down for an extended period. During such a period one can make that check "non-applicable" 
(instead of e.g. having to delete and later regenerate that entry).
* a toggle whether the node is *active* (i.e. whether the request is sent regularly). 
Next to that toggle are two numeric fields that allow to specify the period of the requests (in seconds) 
the timeout (also in seconds), i.e. the time after which a response is considered as degraded. 
If there is no response at all the request is considered as failed (one can specify further conditions 
as to when a request is "OK" or "Failed" - see further down).
There also is a *properties* field that allows to specify `name:value` pairs which can be used as 
placeholders in most alphanumeric entry fields of this node or any of its children nodes, i.e. properties 
are inherited from a node's parent (and grand-parent, etc.).

Besides these generic fields each request type has its specific fields:

<<to be completed>>


### Configuration via a JSON Config File

KISMON saves its configurations in JSON files (default extension is ".kmc" for "_k_is_m_on _c_onfiguration").
This file is a 1:1 serialization of the internal tree structure that steers the operation of the application
(remark: this fact is also the reason why some nodes need a 'className="..."' field because the actual 
(sub)class to be instantiated needs to be signaled to the de-serialization and can not be derived from 
the context).
The fields of each object correspond mostly directly to the form-fields, i.e. it should be relatively 
easy and straight forward to understand what the misc. fields mean.

There is one noteworthy exception - namely a feature for which no GUI exists (yet) and which can thus 
*only* be configured via the config file:

#### certificateHandling (at the very end of the config file):
```
	...
	"certificateHandling" : {
		"descriptors" : [ {
			"hostPattern" : "zhs-base-command-bridge-zhm-egov-test2.zhm.acp2.aspectra.com",
			"keyAndCertFileName" : "D:/Projects/ZHServices/ZHServices Certificates/TEST/zhs-base-command-bridge_client_test.p12",
			"keyAndCertFileType" : "PKCS12",
			"keyAndCertFilePwd" : "changeit",
			"keyAndCertKeyPwd" : "changeit"
		}, {
		...
		} ]
	}
}
```

This feature allows to configure specific TLS client certificates to be used when accessing an other 
system, something that was needed for ZHServices where the access to the command-bridge feature requires 
that the client does not present its own "normal" TLS certificate when accessing the service but a 
specific client certificate that is known to the server.
`hostPattern` specifies the host for which the setting is to be applied 
(Note that in spite the name wildcards or patterns are not yet supported. I plan to implement "patterns" 
in a later version. Right now the name has to be the target's FQDN (fully qualified domain name)).
The other four fields specify the keystore containing that certificate to be used and parameters required
to access to it. Their names should hopefully be self-explanatory.

## Running the Application

### Running on the command line:

... To be completed ...

## Implementation

KISMON is based on Vaadin [Vaadin](https://vaadin.com/), a Graphik library that originally had its roots 
in [GWT (Google Web Toolkit)](https://www.gwtproject.org/) but has since left this ancestry behind 
(since v8+, we are now at v23) and is now working completely without any GWT legacy. 
The current version I would describe as "GWT done right".
The concept still follows the GWT approach that developers can write *the entire* WebApplication in Java 
and the client code gets automatically translated to JavaScript-code which is then shipped to the user's 
browser and is executed there.
The modern Vaadin uses very thin Java wrappers around WebComponents, i.e. the standard approach to 
communicate with modern WebBrowsers and JavaScript components. When using the default widgets the 
developer does not have to deal with such wrappers since Vaadin comes with an big library of basic 
widgets but also with more complex ones like tables and tree views and entire graphic views. 
For KISMON I did not have to deal with a single line of JavaScript. 
The appearance of these widget can be controlled to a large extent using CSS (style sheets). With these 
one can influence the appearance of a UI without having to modify much in the source code.
 
Vaadin also comes with very convenient default classes for application and security configuration and - a 
real highlight of Vaadin - very powerful mappers which populate form fields with corresponding POJO values 
and vice-versa.
With these the mappings updates flow in both directions (i.e. the form is updated when the POJO-value 
changes and the POJO gets updated, when a user modifies a field). These mappers also support format 
conversions and validations of a Java class's field and are really very convenient to use.

### Class-structure

The main overview shows the split into the UI part, the entities and the "backend" and a few utility 
classes (for HTTP-security config, etc.).
At program startup the backend loads one or more configuration files and starts the "monitoring engine".
This part is completely independent and agnostic of the front-end/GUI and can also run completely without 
any UI. The idea is of course that this can run most of the time without anyone looking at it.

When a user connects a new session is created and with it a GUI for that session. The GUI communicates 
with the entities, i.e. it visualizes their status and also allows to modify certain settings.
While a session is active the entities and the UI(s) is/are synchronized, i.e. the UI updates live when 
attributes or status change

![alt KISMON SW structure](docs/kismon_puml.svg "Main SW structure")

![alt KISMON Entities & Views](docs/entities_views_puml.svg "KISMON Entities & Views")

![alt KISMON Entity classes](docs/entities_nodes_puml.svg "Entity classes")

![alt KISMON UI (forms) classes](docs/ui_view_nodes_puml.svg "KISMON UI (forms) classes")


### Running from an IDE:
There are two ways to run the application:  using `mvn spring-boot:run` or by running the `Application` 
class directly from your IDE.

You can use any IDE of your preference, but Vaadin suggests Eclipse or IntelliJ IDEA.
Below are the configuration details to start the project using a `spring-boot:run` command. 
Both, Eclipse and IntelliJ IDEA, are covered.

#### Eclipse
- Right click on a project folder and select `Run As` --> `Maven build..` . 
After that a configuration window is opened.
- In the window set the value of the **Goals** field to `spring-boot:run` 
- You can optionally select `Skip tests` checkbox
- All the other settings can be left to default

Once configurations are set clicking `Run` will start the application

#### IntelliJ IDEA
- On the right side of the window, select Maven --> Plugins--> `spring-boot` --> `spring-boot:run` goal
- Optionally, you can disable tests by clicking on a `Skip Tests mode` blue button.

Clicking on the green run button will start the application.

After the application has started, you can view your it at http://localhost:8085/ in your browser.

If you want to run the application locally in the production mode, use `spring-boot:run -Pproduction` 
command instead.

## Project overview

Project follow the Maven's [standard directory layout structure](https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html):
- Under the `srs/main/java` are located Application sources
   - `Application.java` is a runnable Java application class and a starting point
   - `MainView.java` is a default view and entry point of the application
- Under the `srs/test` are located test files
- `src/main/resources` contains configuration files and static resources, most notably the 
which is used to configure the application for your environment.

- The `frontend` directory in the root folder contains client-side dependencies and resource files
   - All CSS styles used by the application are located under the root directory `frontend/styles`    
   - Templates would be stored under the `frontend/src`


## More Information on Vaadin

- Vaadin Basics [https://vaadin.com/docs](https://vaadin.com/docs)
- More components at [https://vaadin.com/components](https://vaadin.com/components) and 
[https://vaadin.com/directory](https://vaadin.com/directory)
- Download this and other examples at [https://vaadin.com/start](https://vaadin.com/start)
- Using Vaadin and Spring [https://vaadin.com/docs/v14/flow/spring/tutorial-spring-basic.html](https://vaadin.com/docs/v14/flow/spring/tutorial-spring-basic.html) article
- Join discussion and ask a question at [https://vaadin.com/forum](https://vaadin.com/forum)

## Notes

If you run application from a command line, remember to prepend a `mvn` to the command.
