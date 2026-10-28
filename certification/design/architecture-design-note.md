# Architecture design note — <Team name>

> Template. Replace every `<...>`, delete this note and keep the six headings exactly as they are. Between 1,200 and 2,000 words, written by you. Text left as `<...>` counts as missing.

## 1. Target architecture

![Target architecture](target-architecture.png)

| Component in the diagram | Defined in (file of your team's -docs) |
|---|---|
| <service, database or broker> | <05-architecture/...md> |

## 2. Five principles

| Principle | Current weakness | Evidence in -docs | Design decision | Huawei Cloud service |
|---|---|---|---|---|
| Reliability | <...> | <path in -docs> | <...> | <...> |
| Security | <...> | <path in -docs> | <...> | <...> |
| Performance efficiency | <...> | <path in -docs> | <...> | <...> |
| Cost | <...> | <path in -docs> | <...> | <...> |
| Operations | <...> | <path in -docs> | <...> | <...> |

## 3. Scalability

| Service | Scaling metric | Threshold (number and unit) | Min instances | Max instances |
|---|---|---|---|---|
| <service 1> | <...> | <...> | <n> | <n> |
| <service 2> | <...> | <...> | <n> | <n> |

## 4. Disaster recovery

- Critical data store: <...>
- RTO: <minutes> minutes
- RPO: <minutes> minutes
- DR pattern (chapter 11): <...>
- Why the pattern meets those numbers: <two sentences>

## 5. Automated deployment

| Stage | Trigger | Tool or service | File in your team's repositories |
|---|---|---|---|
| Build | <...> | <...> | <path> |
| Test | <...> | <...> | <path> |
| Image | <...> | <...> | <path> |
| Deploy to qa | <...> | <...> | <path> |
| Deploy to production | <...> | <...> | <path> |
| Rollback | <...> | <...> | <path> |

## 6. Decisions

- [ADR-C3-01](ADR-C3-01.md): <decision>
- [ADR-C3-02](ADR-C3-02.md): <decision>
