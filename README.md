# atlassian-metrics

The below steps will take you through some basic proof-of-concept of scraping data from Jira in two different ways. After that you just need to figure out why you want metrics, the purpose of such and then design the data you need.

If we go for Prometheus (inspired by a free availabel add-on for Jira) and Grafana for graphing we can easily get data from Jira in two different ways:

* Using the [Prometheus Exporter For Jira](https://marketplace.atlassian.com/apps/1217960/prometheus-exporter-for-jira?hosting=server&tab=overview) add-on, which will expose some key metrics on a REST endpoint. Most of the metrics are of operational interest.
* Using the [ScriptRunner for Jira](https://marketplace.atlassian.com/apps/6820/scriptrunner-for-jira?hosting=cloud&tab=overview) add-on, where you can serve customize JQL results on a REST endpoint as well. You just need to make data available as JSON for Prometheus.


## 3 components to serve you

This proof-of-concept is based on three components:

* Atlassian Jira - where we want to get data from.
* [Prometheus](https://prometheus.io/) - Monitoring system and time series database - we use persist our data.
* [Grafana](https://grafana.com/) - The Open platform for beautiful analytics and monitoring - that helps us do graphing on the Prometheus data.




### The 3 overall steps

* Get Jira up and running with the two add-ons: Prometheus Exporter and ScriptRunner
  * Configure JQL and add ScriptRunner script to expose on REST
* Get Prometheus running using Docker
* Get Grafana running using Docker and configure graphs


## Get Jira running using Team in Space (Praqma only)

Notice we're an Atlassian partner in Praqma, which means we have access to their Teams in Space which is a solution with all their tools integrated and with realistic data already inside. We dockerized it ourselves to use it for demonstration. We're not allowed to make it public.

_If you're not a Praqmate, you'll have to use your own Jira and install the add-ons. Also adjust the URL and configuration below._

* Start Jira from our Teams in Space container, using the adjusted image that includes Prometheus Exporter for Jira add-on.

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

_Results_: Both queries should respond with the expected result in json: `{"inactive_issues_count":89}`

Should you experience issues with the above queries, we once had problems with browser and needed to use Chrome to run the script once, before Firefox could access the REST endpoint. Consider switching to Chrome if the queries doesn't work.

## Prometheus

We will run Prometheus from docker, but before doing so you need to adjust the configuration file so it points to your Jira server, DNS or IP.

Since we run our Teams in Space in Docker, we use the IP since other docker containers like Prometheus doesn't know about the DNS we use for it.

    $ docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' teams_in_space
    172.17.0.2

Write yourself a new `prometheus.yml` by copying our template (`prometheus.org`) and inserting the Jira server address. Or simple run:

    sed 's/INSERT_JIRA_IP_OR_DNS_NAME_HERE/172.17.0.2/' < prometheus.org > prometheus.yml

Then start it using the configuration, running the following from the root of this repository where your new `prometheus.yml` file is:

    docker run --name prometheus -p 9090:9090 -v $(pwd):/etc/prometheus/ prom/prometheus

* Access Prometheus on the browser with the below link, or on the ip-address you get by running inspect to catch ip again:

    $ docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' prometheus
    172.17.0.3

`http://172.17.0.3:9090/` or `http://localhost:9090` (usually also works with default docker configurations)


* Since our configuration already configures targets in Prometheus, make sure they   has green `UP` state. Find them under `Status -> Targets`

![prometheus_targets](images/prometheus_targets.png)

* Select one of the metrics to be executed under the `Graph`-menu, [direct link](http://localhost:9090/graph).

For select and then execute the `jvm_memory_bytes_used` metric, which is exposed by the Prometheus Exporter add-on. [Direct link](http://localhost:9090/graph?g0.range_input=1h&g0.stacked=1&g0.expr=jvm_memory_bytes_used&g0.tab=0)

You can then click graph to show simple graphs in Prometheus itself.

![prometheus_graph_jvm_memory_bytes_used](images/prometheus_graph_jvm_memory_bytes_used.png)

## Grafana

Grafana have more power for graphing so let's try to fire that up and see a simple graph.


* Run Grafana container image.

```shell

docker run \
--detach \
--name grafana \
--publish 3000:3000 \
grafana/grafana

```

* Access Grafana on the browser with the below link or use inspect again as above to catch IP address:

`http://localhost:3000/` (login, default default username and password from the official hub image (`admin` and pass `admin`)

Now we will add Prometheus data sources....

* Click `Add data sources` button.

![grafana_datasources_new](images/grafana_datasources_new.png)

* Use the below configuration.

Name: `jira`

Type: `Prometheus`

URL:    `http://172.17.0.3:9090` (**Replace with your Jira server address from above steps**)

Access: `Browser`

![grafana_datasources_new_configuration](images/grafana_datasources_new_configuration.png)

Click `Save & Test` button. Then, `Data source is working` notification should be shown.

Now we're going to create a dashboard:

* Click on `Create` or `+` on the top left area. Select `Import` on the pop-up menu.

![grafana_create_import](images/grafana_create_import.png)

We will use the dashboard template from this [project](https://github.com/AndreyVMarkelov/jira-prometheus-exporter).

The dashboard template has `5249` as its ID.

Enter `5249` into the field of `Grafana.com Dashboard`. Click `Load` button.

![grafana_create_import_load](images/grafana_create_import_load.png)

* Select `jira` in `prometheus` field under `Options`.

![grafana_create_import_load_jira](images/grafana_create_import_load_jira.png)

* Click `Import` button.

* Dashboard reflecting Jira metrics will be shown similarly like the below image.

![grafana_dashboard_jira](images/grafana_dashboard_jira.png)
