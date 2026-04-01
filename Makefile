.PHONY: ad-run ad-debug seed

ad-run:
	./scripts/android-run.sh normal

ad-debug:
	./scripts/android-run.sh debug-ui

seed:
	./scripts/generate_local_knowledge_benchmark.py
