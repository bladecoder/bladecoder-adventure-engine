# Optional repository initialization and publication

File generation, building, Git initialization and remote publication are separate
operations. A setup run creates useful files even when Git or account access is
unavailable. Inspect the generated project before initializing it:

```sh
java -jar /path/to/setup.jar git-init --project /path/to/game --branch main --json
```

This initializes a local repository only. It creates no commit and never resets
an existing repository or nests one under a different repository automatically.
Review sources, asset licenses, `.gitignore` and ignored personal tool settings;
commit the intended content separately. Existing branch names remain unchanged.

Remote creation is an explicitly selected operation using an independently
installed/authenticated GitHub CLI. Supply the exact owner/name and visibility:

```sh
java -jar /path/to/setup.jar github-create --project /path/to/game --repository OWNER/NAME --visibility private --out /new/publication-report.json --json
```

Without `--push`, this creates and connects the remote without publishing local
commits. Selecting `--push` additionally publishes existing commits, requiring a
clean committed working tree. Setup never commits game content implicitly.
An existing `origin` is rejected so an earlier or partially completed attempt
cannot silently redirect it. Authentication/account selection and hosted-agent
launch are separate; ordinary local setup has no account dependency.

Keep the report outside the repository. The operation writes a journal before
attempting remote creation and records completion or partial failure. On a
failure after creation begins, the remote may already exist: inspect the selected
remote and local `origin` before retrying. Generated files and commits remain;
there is no automatic deletion or remote rollback. Do not publish a private
session store or personal configuration while preparing commits.

The local initialization command is verified separately from hosted publication.
Simulated command-transport tests cover authentication failure, a partially
created remote, existing-origin rejection, clean-commit preconditions and explicit
visibility/push selection. They do not prove a live GitHub publication. Retain
actual hosted-operation results when you deliberately select that operation.
