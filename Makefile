SHELL := /bin/bash

.PHONY: run client job_service install install-client install-job_service db stop

run: install
	@trap 'kill 0' SIGINT TERM; \
	$(MAKE) job_service & \
	$(MAKE) client & \
	wait

client: install-client
	cd client && npm run dev

job_service: install-job_service
	cd job_service && mvn -q compile org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=job.Main

install: install-client install-job_service

install-client:
	cd client && npm install

install-job_service:
	cd job_service && mvn -q dependency:resolve

db:
	docker compose up -d cockroach

stop:
	docker compose down
