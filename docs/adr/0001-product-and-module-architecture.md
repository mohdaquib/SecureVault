# ADR 0001: Product and module architecture

- Status: Accepted
- Date: 2026-08-20
- Decision owners: SecureVault maintainers

## Context

SecureVault currently demonstrates encrypted Android storage, key management, certificate pinning, and tests that exercise those controls. The product may also expose reusable runtime security controls and build-time auditing to consuming Android applications.

Without an explicit boundary, security implementation, audit rules, commercial packaging, and claims about compliance can become coupled. That would make the API harder to evolve, encourage dependencies in the wrong direction, and risk presenting test output as a certification.

This decision defines the intended product and module boundaries. Proposed modules in this ADR are architectural targets; their presence here does not mean they have been implemented or included in the Gradle build.

## Decision

### Product scope

SecureVault is an Android security toolkit and reference application. Its reusable product surface is responsible for:

- protecting application secrets with Android-backed key management;
- providing encrypted local-data primitives;
- applying explicit network-security policy, including certificate pinning;
- reporting the observable state and result of those controls through stable, machine-readable audit contracts; and
- checking application configuration and SecureVault integration during development and CI.

The reference application demonstrates composition of these capabilities. It is not itself the reusable SDK API.

SecureVault can produce **compliance evidence**: versioned, attributable observations such as configured controls, check results, timestamps, inputs, and tool versions. This evidence can support an organization's compliance process. SecureVault does not issue **compliance certification**, attest that an organization satisfies a law or standard, or replace assessment by an authorized auditor.

### Module responsibilities

The target reusable architecture contains the following modules. A module is justified only when it creates a technical boundary in platform dependencies, API stability, runtime behavior, or build lifecycle.

| Module | Responsibility | Technical reason for a separate module |
| --- | --- | --- |
| `securevault-audit-api` | Platform-neutral audit result, finding, severity, control identifier, evidence metadata, and reporter contracts. | Audit producers and consumers need a small, stable contract without Android, crypto, networking, Gradle, or rule-engine dependencies. This also permits build-time tools to emit the same schema as runtime components. |
| `securevault-core` | Android runtime orchestration, configuration, lifecycle, capability registration, and aggregation of runtime audit observations. | This is the composition boundary used by Android applications. It owns Android lifecycle concerns while depending only on public capability contracts, not concrete crypto or network internals. |
| `securevault-crypto` | Android Keystore integration, encryption/key handling, and crypto-specific runtime evidence. | Crypto code has specialized platform APIs, threat assumptions, dependency review, testing, and release sensitivity. Isolating it prevents consumers of unrelated controls from inheriting crypto implementation details. |
| `securevault-network` | OkHttp-facing secure client configuration, pinning policy, network checks, and network-specific runtime evidence. | Networking has an independent integration surface and third-party dependency graph. It must be usable and testable without storage or crypto implementation dependencies. |
| `securevault-audit-checks` | Static/build-time checks over manifests, resources, dependency/configuration metadata, and declared SecureVault policy. | Build-time analysis executes in Gradle/CI rather than in the app process and may depend on lint or build-tool APIs that must never enter runtime artifacts. |

The existing `app`, `domain`, `data`, `core-crypto`, and `core-network` modules remain the reference application's current implementation. During extraction, code moves only when it matches a responsibility above. The similarly named `core-*` and `securevault-*` modules must not coexist indefinitely as duplicate implementations: the former are application-oriented boundaries; the latter are reusable product boundaries.

No module may be added merely to mirror an organization chart, pricing tier, package namespace, or hypothetical future feature. A new module requires an ADR amendment stating its unique dependency or lifecycle boundary.

### Dependency direction

The allowed target dependencies are:

```text
securevault-audit-checks ---> securevault-audit-api

securevault-crypto  -------> securevault-audit-api
securevault-network -------> securevault-audit-api
          ^                         ^
          |                         |
          +------ securevault-core -+

app ---> securevault-core
app ---> selected capability modules
```

Arrows mean “depends on.” More specifically:

- `securevault-audit-api` depends on no other SecureVault module and remains free of Android and build-tool types.
- `securevault-crypto` and `securevault-network` depend on `securevault-audit-api`, but not on each other.
- `securevault-core` depends on `securevault-audit-api` and capability contracts. Concrete capabilities are supplied by composition or registration; core does not reach into their internals.
- `securevault-audit-checks` depends on `securevault-audit-api`, never on a runtime Android implementation module.
- The application is the composition root and may select runtime capabilities. Libraries do not depend on `app`, `data`, or `domain`.
- Cyclic project dependencies are prohibited.

If direct project dependencies are needed temporarily during extraction, they must follow the same inward direction and be removed before publishing the reusable artifacts.

### Public versus internal APIs

Public API consists only of documented types required to configure SecureVault, invoke supported operations, register capabilities, or consume audit results. Public types must use SecureVault-owned or platform-stable types at their boundaries; implementation-specific OkHttp, SQLCipher, Room, Gradle, lint, and cryptographic-provider types are not exposed unless interoperability makes that type the intentional integration contract.

Everything else is internal by default. Kotlin declarations use `internal` where possible. Android resources intended for consumers are explicitly declared public; other resources remain private. Published artifacts receive API-signature checks, and changes to that signature follow the versioning policy below.

`securevault-audit-api` is the cross-lifecycle interchange contract. Runtime modules and build-time checks may implement or produce its types, but consumers must not downcast to implementation classes or rely on undocumented finding payloads.

### Runtime versus build-time auditing

Runtime auditing observes facts that only the running application can establish, such as successful key access, active pinning policy, provider state, or execution of an encryption operation. Runtime checks must be bounded, avoid collecting secrets, and describe when and how an observation was made.

Build-time auditing analyzes declared or packaged state, such as manifest configuration, dependency presence, resource policy, or required SecureVault setup. Build-time checks may fail CI according to an explicit severity policy, but their implementation and build-tool dependencies never ship in the application.

Neither lifecycle may claim facts visible only to the other. Results share the `securevault-audit-api` schema and identify their origin as `RUNTIME` or `BUILD_TIME`. Combined reports preserve that origin, the producing component and version, relevant non-secret inputs, and collection time. “Check passed” means only that the named check found its documented condition in its documented scope.

### Free, Pro, and Enterprise boundaries

Free, Pro, and Enterprise are product packaging and entitlement levels, not architectural layers or arbitrary source-code folders. There will be no `free`, `pro`, or `enterprise` source module merely to hide otherwise identical code.

- **Free** packages the baseline runtime controls, local results, and a useful set of build-time checks for individual applications.
- **Pro** packages advanced policy, richer evidence retention/export, and team-oriented workflows.
- **Enterprise** packages organization-scale policy administration, fleet aggregation, identity/access integration, support commitments, and deployment or governance features.

Capabilities are assigned to tiers in product metadata, licensing/entitlement policy, service configuration, or distribution manifests. The underlying technical module remains organized by responsibility. A separately shipped enterprise connector is acceptable only when its external SDK, deployment model, permissions, or release lifecycle independently justifies a module; its name describes that integration, not the price tier.

Entitlement checks occur at product entry points and fail explicitly. They do not fork security algorithms, weaken baseline security, or scatter tier conditionals through low-level crypto and network implementations. Evidence always states which checks ran and which were unavailable; an unavailable paid check is not reported as passed.

### Explicit non-goals

This architecture does not aim to:

- certify compliance with any law, regulation, framework, or customer policy;
- guarantee that an application is secure or free from vulnerabilities;
- replace penetration testing, code review, threat modeling, legal advice, or independent audit;
- provide a general-purpose identity, secrets-management backend, VPN, firewall, or mobile-device-management system;
- invent custom cryptographic primitives or protocols;
- hide security behavior behind a pricing tier when that behavior is necessary to use a baseline control safely;
- make build-time analysis infer runtime facts, or runtime analysis reconstruct the build environment;
- create a module for every feature, customer, pricing tier, package, or control; or
- preserve the current sample module layout when doing so would duplicate the target reusable modules.

### Versioning policy

Published SecureVault artifacts use Semantic Versioning as one coordinated release train. All modules released from a repository revision use the same version, even when a module has no source change, so evidence can identify a coherent toolset.

- A **major** release may remove or incompatibly change public source, binary, resource, serialized audit-schema, finding-identifier, or documented behavioral contracts.
- A **minor** release adds backward-compatible APIs, capabilities, findings, or optional evidence fields. New checks that can fail an existing default build are not enabled as errors in a minor release without an explicit consumer opt-in.
- A **patch** release contains backward-compatible fixes and documentation changes. Tightening a check in a way that newly fails consumers is not treated as a patch unless it corrects a security defect; in that case the release notes identify the security impact and migration path.

Experimental APIs are visibly annotated, excluded from compatibility guarantees, and cannot be the only way to perform a baseline security operation. Deprecation precedes removal for at least one minor release unless retaining the API would create a material security risk. Audit schema and finding identifiers remain readable across supported major versions, or a documented migration is supplied.

Pricing and entitlement changes do not require an SDK major version unless they also alter a technical compatibility contract. Evidence records the SDK/module version and rule-set version separately so rule updates do not masquerade as binary API changes.

## Consequences

- Runtime consumers do not acquire Gradle or lint dependencies.
- Audit reporters can consume one schema across build-time and runtime producers.
- Crypto and network integrations can evolve and be tested independently without forming parallel product tiers.
- Commercial packaging can change without reorganizing source code or destabilizing module APIs.
- The project must maintain public API signatures, finding identifiers, evidence provenance, and coordinated release versions.
- Extraction from the reference application requires deliberate migration and removal of duplicate implementations.

## Acceptance criteria

This decision is satisfied when:

- every proposed module has the technical reason stated in the module table and no module exists solely for organizational or commercial convenience;
- dependency checks enforce the direction in this ADR and runtime artifacts contain no build-time auditing implementation;
- published public APIs are documented and signature-checked, with other declarations internal by default;
- Free, Pro, and Enterprise capabilities are expressed through packaging and entitlement metadata rather than arbitrary tier source folders;
- runtime and build-time evidence identifies its origin, producer version, scope, and time without exposing secrets;
- product documentation consistently calls generated output “compliance evidence” and does not describe it as “compliance certification”;
- unavailable, skipped, and failed checks remain distinguishable from passed checks; and
- releases follow the compatibility and versioning rules in this ADR.
