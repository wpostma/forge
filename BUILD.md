# Developer Build Commands

This project is a Maven multi-module reactor rooted at `pom.xml`.

Maven does not provide a project-specific `make help` style command that lists
only the commands this repository supports. Instead, the practical inventory is:

- Maven lifecycle phases that can be run from the root.
- Reactor modules declared in the root `pom.xml`.
- Profiles declared in module POMs.
- Plugin goals configured or commonly useful for this build.

The repository also has `.mvn/maven.config`, which tells Maven to use
`./.mvn/local-settings.xml` automatically.

## Basic Commands

Run these from the repository root:

```powershell
mvn clean
mvn validate
mvn compile
mvn test
mvn package
mvn verify
mvn install
mvn deploy
mvn site
```

Common developer builds:

```powershell
mvn clean install
mvn -pl forge-gui-desktop -am -DskipTests compile
mvn -pl forge-gui-desktop -am package
mvn -pl forge-game test
mvn -pl forge-core,forge-game,forge-ai install
```

Notes:

- `install` is a Maven lifecycle phase. It is valid here because Maven supports
  it for the reactor, not because this repository declares an `install` target.
- `-pl` selects one or more reactor modules.
- `-am` also builds dependencies needed by the selected modules.

## Reactor Modules

The root POM declares these modules:

```text
forge-core
forge-game
forge-ai
forge-gui
forge-gui-mobile
forge-gui-mobile-dev
forge-gui-desktop
forge-gui-ios
forge-lda
adventure-editor
forge-gui-android
forge-installer
```

Examples:

```powershell
mvn -pl forge-gui-desktop -am package
mvn -pl forge-gui-android -am package
mvn -pl adventure-editor -am package
```

## Profiles

List all profiles Maven can see:

```powershell
mvn help:all-profiles
```

List profiles active for the current invocation:

```powershell
mvn help:active-profiles
```

Profiles currently declared by this project:

```text
forge-gui-desktop:
  osx
  osx-release
  android-test-build

forge-gui-android:
  android-debug
  android-release-build
  android-release-upload
  android-test-build
  android-dev-build

forge-installer:
  windows-linux
  no-flatten
```

Examples:

```powershell
mvn -Pandroid-debug package
mvn -Pandroid-release-build install
mvn -Pwindows-linux verify
mvn -Pno-flatten install
```

See `docs/Development/Android-Builds.md` for Android signing and upload
details. Release builds may need credentials, signing files, or environment
variables that are intentionally not committed to the repository.

## Useful Plugin Goals

These are useful direct goals for inspecting or validating the build:

```powershell
mvn help:effective-pom
mvn help:all-profiles
mvn help:active-profiles
mvn dependency:tree
mvn dependency:analyze
mvn checkstyle:check
mvn enforcer:enforce
```

Release-oriented plugin goals are also configured:

```powershell
mvn release:prepare
mvn release:perform
```

Use release goals carefully. They can modify versions, create tags, and perform
SCM operations depending on configuration and command-line options.

## Bound Plugin Executions

The root POM binds these notable plugin executions:

```text
validate:
  maven-enforcer-plugin:enforce
  maven-checkstyle-plugin:check

clean:
  flatten-maven-plugin:clean

deploy:
  flatten-maven-plugin:flatten
```

Several modules bind additional packaging or preparation steps, including:

```text
adventure-editor:
  package -> replacer:replace
  package -> maven-assembly-plugin:single

forge-gui-mobile-dev:
  initialize -> build-helper-maven-plugin timestamp/regex properties
  package -> launch4j-maven-plugin:launch4j
  package -> replacer:replace
  package -> maven-assembly-plugin:single

forge-gui-android:
  initialize -> build-helper-maven-plugin timestamp/regex properties
  generate-sources -> git-changelog-maven-plugin:git-changelog
  package/verify -> Android release/debug profile tasks

forge-gui-desktop:
  osx/osx-release profiles add app bundle and distribution attachment tasks
  android-test-build profile adds git changelog generation

forge-installer:
  initialize -> replacer:replace
  windows-linux profile adds installer preparation, app bundle tasks, and artifact attachment
```

For the exact merged configuration, run:

```powershell
mvn help:effective-pom
```

## Observed Warnings

While running Maven help goals in this checkout, Maven printed warnings about:

```text
org.apache.maven.wagon:wagon-ftp:3.5.3
org.eclipse.m2e:lifecycle-mapping:1.0.0
```

The help goals still completed successfully. The `wagon-ftp` warning appears
because it is configured as a build extension/dependency rather than a normal
Maven plugin with a plugin descriptor. The `lifecycle-mapping` warning is for
Eclipse m2e metadata and is not expected to affect command-line builds.

## Quick Discovery Commands

Useful commands when changing the build:

```powershell
mvn help:effective-pom
mvn help:all-profiles
mvn help:describe -Dplugin=org.apache.maven.plugins:maven-dependency-plugin -Ddetail
mvn help:describe -Dplugin=org.apache.maven.plugins:maven-checkstyle-plugin -Ddetail
```

There is no single authoritative Maven command that prints every meaningful
repository-specific invocation. When in doubt, inspect `pom.xml`, module POMs,
`.mvn/maven.config`, and the output of `mvn help:effective-pom`.
