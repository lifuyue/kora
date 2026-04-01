.PHONY: ad-run ad-debug seed seed-e2e

ad-run:
	./scripts/android-run.sh normal

ad-debug:
	./scripts/android-run.sh debug-ui

seed:
	./scripts/seed-installed-app.sh

seed-e2e:
	./scripts/seed-e2e.sh
