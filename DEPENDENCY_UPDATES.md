# Dependency updates — October 24, 2026

The repository uses React/Vite and Java/Spring Boot. There is no PHP dependency to update here.

## Applied repository updates

| Component | Updated version |
| --- | --- |
| React / React DOM | 19.3.1 |
| React Router | 8.5.0 |
| Vite / React plugin | 8.3.5 / 6.1.2 |
| ESLint / typescript-eslint | 10.13.0 / 8.72.0 |
| TypeScript | 6.0.3 |
| Node / React / React DOM typings | 24.19.2 / 19.3.0 / 19.3.0 |
| React Refresh lint plugin / globals | 0.5.7 / 17.13.0 |
| brace-expansion / source-map-js / zod | 5.0.12 / 1.2.2 / 4.6.7 |
| Spring Boot | 4.1.2 |
| Tomcat / Jackson | 11.0.27 / 3.1.7 |
| Spring Security / PostgreSQL JDBC / Lombok | 7.1.2 / 42.7.14 / 1.18.48 |
| Maven distribution | 3.10.0, with SHA-256 verification |

All frontend packages were refreshed within the updated manifest ranges. `@eslint/js` 10.0.1, `eslint-plugin-react-hooks` 7.1.1, and Maven Wrapper 3.3.4 were already current. Backend libraries and plugins remain managed by the Spring Boot dependency set; no individual version overrides were added.

TypeScript remains on the latest compatible 6.x version because typescript-eslint 8.72.0 requires TypeScript below 6.1. Node typings remain on 24.x to match the selected runtime. `frontend/.nvmrc` pins Node 24.22.1, and `package.json` requires patched Node 24.x.

The Windows Maven wrapper also handles an ordinary, non-symlink Maven cache without indexing a null `Target` value.

## Install the machine runtimes

Validation used checksum-verified portable Node 24.22.1 (including npm 11.19.1), Azul Zulu JDK 21.0.13, and Maven 3.10.0 in a temporary directory. The existing system installations were not replaced.

Run these commands in an Administrator PowerShell window to install or update the system runtimes to the validated versions:

```powershell
winget install --id OpenJS.NodeJS.LTS --exact --version 24.22.1 --source winget
winget install --id Azul.Zulu.21.JDK --exact --version 21.54.107 --source winget
```

Open a new terminal after installation. Select the new JDK for this PowerShell session and save `JAVA_HOME` for future sessions. If you selected a custom installation directory, adjust the path:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Zulu\zulu-21'
$env:Path = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:Path
[Environment]::SetEnvironmentVariable('JAVA_HOME', $env:JAVA_HOME, 'User')
node --version
npm.cmd --version
java -version
```

Ensure your IDE and any launch scripts also use this JDK. The persistent Windows `PATH` may still select the old Java installation; the session command above explicitly selects JDK 21, and Maven uses `JAVA_HOME`. Use the repository's Maven wrapper to select Maven 3.10.0; a separate global Maven upgrade is unnecessary.

## Apply the Java certificate-trust mitigation

The [October 23 Java guidance](https://ops.java/emudhra) recommends removing the two affected trust roots or enabling revocation checking, even for the first emergency runtime updates. Validation used the trust-root removal option with a backup. To apply the same mitigation to the installed JDK, run the following in Administrator PowerShell after setting `JAVA_HOME` above:

```powershell
$jdkKeytool = Join-Path $env:JAVA_HOME 'bin/keytool.exe'
$jdkTrustStore = Join-Path $env:JAVA_HOME 'lib/security/cacerts'
$jdkTrustBackup = $jdkTrustStore + '.before-emudhra'
& $jdkKeytool -list -keystore $jdkTrustStore -storepass changeit *> $null
if ($LASTEXITCODE -ne 0) { throw 'Check JAVA_HOME and the trust-store password before proceeding.' }
if (-not (Test-Path -LiteralPath $jdkTrustBackup)) {
    Copy-Item -LiteralPath $jdkTrustStore -Destination $jdkTrustBackup
}
foreach ($jdkAlias in @('emsigneccrootcag3 [jdk]', 'comodoaaaca [jdk]')) {
    & $jdkKeytool -list -keystore $jdkTrustStore -storepass changeit -alias $jdkAlias *> $null
    if ($LASTEXITCODE -eq 0) {
        & $jdkKeytool -delete -keystore $jdkTrustStore -storepass changeit -alias $jdkAlias -noprompt
        if ($LASTEXITCODE -ne 0) { throw "Could not remove $jdkAlias" }
    }
}
```

This assumes the distribution's default trust-store password, `changeit`. The backup preserves the original trust store. Restart applications using the JDK after this change. [Azul also documents a vendor-specific blocklist workaround](https://docs.azul.com/core/emudhra-workaround).

## Install and verify repository dependencies

Stop running development servers so Windows can replace any locked native modules. Run from the repository root after selecting the updated runtimes:

```powershell
Push-Location frontend
npm.cmd ci
npm.cmd run build
npm.cmd run lint
npm.cmd audit
Pop-Location

Push-Location test-tracker
.\mvnw.cmd --version
.\mvnw.cmd -B -ntp clean verify
Pop-Location
```

`npm ci` installs every frontend dependency, including the patched transitive packages, from the committed lockfile. The Maven wrapper downloads the pinned Maven distribution, verifies its checksum, and resolves the Spring Boot-managed dependencies. The full backend test suite needs the database configuration described in `test-tracker/.env.local.example`.

For future compatible frontend updates, use `npm.cmd update --save` in `frontend`, then rerun build, lint, and audit. Review a new stable Spring Boot release before changing the parent version in `test-tracker/pom.xml`; this updates its managed libraries and plugins together. When updating the Maven distribution URL, update `distributionSha256Sum` to the verified checksum of that exact archive.

## Validation results

- Frontend production build and ESLint passed under Node 24.22.1.
- npm reported zero known vulnerabilities after installing the updates.
- Maven clean verification passed: 22 tests, zero failures, zero errors, zero skipped.
- OSV reported no matching advisories for the 126 resolved backend dependency versions checked on October 24, 2026.

Restart running development servers and rebuild/redeploy deployed applications to use the updated dependencies and runtimes.
