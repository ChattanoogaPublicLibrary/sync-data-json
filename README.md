# sync-data-json

A utility to mirror datasets from other data portals as external datasets in your Socrata Open Data Portal. Uses external data portal's data.json dataset catalog for aggregation.

## Installation

This was setup for [Heroku](https://www.heroku.com), so that is the suggested platform. Otherwise, you will need to provide leiningen, postgres yourself.

* Install [leiningen](https://github.com/technomancy/leiningen) using its install script (don't use `apt`).
* Clone this project and `cd` into its directory..
* Add an application to Heroku.
* Add a Heroku git remote to this project:

```bash
heroku git:remote -a yourherokuapp`
```

* Add [Postgres](https://www.heroku.com/postgres) to your application:

```bash
heroku addons:add heroku-postgresql:dev
```

* Set the [environment variables](#environment-variables) for the application, substituting your own values for those shown:

```bash
heroku config:add SYNC_DATA_JSON_URL=http://some.remotedataportalyourepullingfrom.com/data.json
heroku config:add SYNC_HOST=some.remotedataportalyourepullingfrom.com
heroku config:add SYNC_URL=http://your.socratadataportal.com
heroku config:app SYNC_USERNAME=yourusername@place.com
heroku config:app SYNC_PASSWORD=yourpassword
heroku config:app SYNC_TOKEN=yoursocrataapplicationtoken
heroku config:app SYNC_ATTRIBUTION="Hosted by the Some Awesome Portal"
heroku config:app SYNC_ATTRIBUTION_URL="http://anawesome.remoteportal.org"
```

* Then set your Postgres details. Find them by visiting your app's Resources tab in Heroku, then clicking the "Heroku Postgres :: Aqua" link. This should take you to a page with your assigned Postgres DB settings.

```bash
heroku config:add DATABASE_HOST=yourhost
heroku config:add DATABASE_NAME=yourdatabase
heroku config:add DATABASE_USER=youruser
heroku config:add DATABASE_PASSWORD=yourpassword
```

* Run the migrations (**locally**). Substitute the all-caps variables below for the same values you set on your Heroku server in the previous step.

```bash
lein ragtime migrate -d "jdbc:postgresql://DB_HOST:5432/DB_NAME?user=DB_USER_NAME&password=DB_USER_PASSWORD&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory"
```

* Push to Heroku to deploy the app:

```bash
git push heroku master
```

* Scale down the the web dyno. We don't need it.

```bash
heroku ps:scale web=0
```

* Add a clock process, because that's what we actually need.

```bash
heroku ps:scale clock=1
```

### Environment Variables

To set an environment variable, run `heroku config:add NAME_OF_VARIABLE=SOMEVALUE` in the root of your project.

* `SYNC_DATA_JSON_URL`
  * The location of the data.json catalog of the data portal you want to pull from.
* `SYNC_HOST`
  * The host of the remote data portal you want to pull from. If the URL is `http://your.dataportal.org` the host would be `your.dataportal.org`.
* `SYNC_URL`
  *  The URL of the data portal you want to pull from. e.g. `http://your.dataportal.org`.
* `SYNC_USERNAME`
  * Your Socrata username.
* `SYNC_PASSWORD`
  * Your Socrata password.
* `SYNC_TOKEN`
  * Your Socrata application token.
* `DATABASE_HOST`
  * The postgres host.
* `DATABASE_NAME`
  * The name of the database in Postgres that the application will be using.
* `DATABASE_USER`
  * The postgres database user to access DB_NAME.
* `DATABASE_PASSWORD`
  * The password for DB_USER_NAME.
* `SYNC_ATTRIBUTION`
  * Attribution to go in the footer of the page for the external dataset.
* `SYNC_ATTRIBUTION_URL`
  * Attribution URL to go in the footer of the page for the external dataset.

## Local development

If you're developing this app locally, you **will** need the following: postgres, leiningen, java (OpenJDK 7 appears to work best.)


### Environment Variables

At the root of your project, add a `.lein-env` file. It will contain an edn config of environment variables defining what they each should be. Here is an example:

```clojure
{
  :database-name "somename"
  :database-user "someusername"
  :database-password "somepassword"
  :database-host "localhost"
  :test-migrations-path "jdbc:postgresql://localhost:5432/somename?user=someusername&password=somepassword"
  :test-url "https://some.dataportal.org"
  :test-username "somename@dataportal.org"
  :test-password "anotherpassword"
  :test-token "somelongtoken"
  :sync-host "anawesome.remoteportal.org"
  :sync-url "http://some.dataportal.org"
  :sync-username "somename@dataportal.org"
  :sync-data-json-url "https://anawesome.remoteportal.org/data.json"
  :sync-password "anotherpassword"
  :sync-token "somelongtoken"
  :sync-attribution "Hosted by the Some Awesome Portal"
  :sync-attribution-url "http://anawesome.remoteportal.org"
}
```

### Tests

Ensure you have a `.lein-env` file and migrate your test database. This database must be different from your application's development database:

```bash
lein ragtime migrate -d "jdbc:postgresql://localhost:5432/somename_test?user=someusername&password=somepassword"
```

Then, you can either run

```bash
lein midje
```

or to start the test runner

```bash
lein midje :autotest
```

## License

This project is licensed under the MIT License. See LICENSE for more details.
