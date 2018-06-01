# atlassian-metrics

## Atlassian Jira

* Start Jira from the TIS container image.

Expect some time for Jira to start up properly. Check the progress of starting Jira under this [page](http://jira.teamsinspace.com:8080).

```bash

docker run \
--name teams_in_space \
--detach \
--interactive \
--tty \
--publish 2430:2430 \
--publish 7990:7990 \
--publish 8060:8060 \
--publish 8080:8080 \
--publish 8085:8085 \
--publish 8090:8090 \
praqma/tis:use-prometheus-plugin \
start-jira.sh

```

* Log into JIRA (credentials for admin can be found in our [Teams in Space project](https://github.com/Praqma/tis)) and do a `Re-Index` under `Jira Administration -> System -> Advanced -> Indexing`. [Direct link](http://jira.teamsinspace.com:8080/secure/admin/jira/IndexAdmin.jspa).

* After indexing completed, check if the JQL query is working properly: `http://jira.teamsinspace.com:8080/issues/?jql=(project%20%3D%20TIS%20and%20assignee%20!%3D%20currentUser()%20and%20updated%20<%3D%20-7d%20and%20resolution%20is%20EMPTY)`

JQL results should return `89` issues.

### ScriptRunner

* Open Script Console page, under `Jira Administration -> Add-ons, left menu chose Script Console under Scriptrunner`: [Direct link ](http://jira.teamsinspace.com:8080/plugins/servlet/scriptrunner/admin/console).

* Paste the content of [get_jira_issues_rest_endpoint.groovy](get_jira_issues_rest_endpoint.groovy) file into the console. Remember to click the `Run` button at the bottom of the console.

* After `Run`, click on the `Logs` tab, you should get a similar output as below.

`2018-06-01 07:52:34,516 DEBUG [runner.ScriptRunnerImpl]: Total issues: 89`

### REST Endpoint

* Use the authenticated browser session to browse below link.

`http://jira.teamsinspace.com:8080/rest/scriptrunner/latest/custom/getInactiveIssues`

* Use a terminal to use the below command.

`curl -u username:password http://jira.teamsinspace.com:8080/rest/scriptrunner/latest/custom/getInactiveIssues`

## Prometheus

* Use pre-compiled binaries Prometheus. Get them [here](https://prometheus.io/download/#prometheus).

* Run Prometheus locally with the below command, at the root of this project.

`prometheus --config.file="prometheus.yml"`

* Access Prometheus on the browser with the below link.

`http://localhost:9090/`

* Make sure that Prometheus target page has green `UP` state.

![prometheus_targets](images/prometheus_targets.png)

* Select one of the metrics to be executed under Prometheus graph [page](http://localhost:9090/graph). For [example](http://localhost:9090/graph?g0.range_input=1h&g0.stacked=1&g0.expr=jvm_memory_bytes_used&g0.tab=0), the `jvm_memory_bytes_used` metric was selected.

![prometheus_graph_jvm_memory_bytes_used](images/prometheus_graph_jvm_memory_bytes_used.png)

## Grafana

* Run Grafana container image.

```shell

docker run \
--detach \
--publish 3000:3000 \
grafana/grafana

```

* Access Grafana on the browser with the below link.

`http://localhost:3000/`

* Login with default username and password.

* Click `Add data sources` button.

![grafana_datasources_new](images/grafana_datasources_new.png)

* Use the below configuration.

Name: `jira`

Type: `Prometheus`

URL:    `http://localhost:9090`

Access: `Browser`

![grafana_datasources_new_configuration](images/grafana_datasources_new_configuration.png)

Click `Save & Test` button. Then, `Data source is working` notification should be shown.

* Click on `Create` or `+` on the top left area. Select `Import` on the pop-up menu.

![grafana_create_import](images/grafana_create_import.png)

We will use the dashboard template from this [project](https://github.com/AndreyVMarkelov/jira-prometheus-exporter).

The dashboard template has `5249` as its ID.

Enter `5249` into the field of `Grafana.com Dashboard`. Click `Load` button.

![grafana_create_import_load](images/grafana_create_import_load.png)

* Select `jira` under `Options`.

![grafana_create_import_load_jira](images/grafana_create_import_load_jira.png)

* Click `Import` button.

* Dashboard reflecting Jira metrics will be shown similarly like the below image.

![grafana_dashboard_jira](images/grafana_dashboard_jira.png)
