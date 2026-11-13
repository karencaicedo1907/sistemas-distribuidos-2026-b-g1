# Certification C3 — Karen Johana Caicedo Arias

## Cover

| Field | Value |
|---|---|
| Full name | Karen Johana Caicedo Arias |
| GitHub user | karencaicedo1907 |
| Group | G1 |
| Team | CineSync Platform |
| Team -docs repository | https://github.com/code-corhuila/csp-docs |
| Course | HCCDP-Solution Architectures Course |
| Course link | https://e.huawei.com/en/talent/ict-academy/#/ict-courses-detail?courseId=6WtKiPd1S1YelBB0AUbE1vXlPgk |
| Start date | 2026-10-30 |
| Completion date | <YYYY-MM-DD> |

## Study log

| Date | Hours | Chapters or labs | Evidence file |
|---|---|---|---|
| 2026-10-30 | 0.17 | Enrollment in the course (10 min) | evidence/00-enrollment.png |
| 2026-10-31 | 1.00 | Chapter 1 | evidence/01-chapter-01.png |
| 2026-10-31 | 1.25 | Chapter 2 | evidence/02-chapter-02.png |
| 2026-10-31 | 3.75 | Chapter 3 | evidence/03-chapter-03.png |
| 2026-11-01 | 3.00 | Chapter 4 | evidence/04-chapter-04.png |
| 2026-11-01 | 3.00 | Chapter 5 | evidence/05-chapter-05.png |
| 2026-11-02 | 3.08 | Chapter 6 | evidence/06-chapter-06.png |
| 2026-11-02 | 2.92 | Chapter 7 | evidence/07-chapter-07.png |
| 2026-11-03 | 2.00 | Chapter 8 | evidence/08-chapter-08.png |
| 2026-11-03 | 3.42 | Chapter 9 | evidence/09-chapter-09.png |
| 2026-11-04 | 4.00 | Chapter 10 | evidence/10-chapter-10.png |
| 2026-11-05 | 3.00 | Chapter 11 | evidence/11-chapter-11.png |
| 2026-11-05 | 3.00 | Chapter 12 | evidence/12-chapter-12.png |
| 2026-11-06 | 4.00 | Chapter 13 | evidence/13-chapter-13.png |
| 2026-11-07 | 4.00 | Chapter 14 | evidence/14-chapter-14.png |
| 2026-11-08 | 1.52 | Lab 1: Cloud Computing and Storage | evidence/15-lab-1.png |
| 2026-11-09 | 1.33 | Lab 2: High-Performance Network Solution | evidence/16-lab-2.png |
| 2026-11-11 | 1.32 | Lab 3: Highly Elastic Service System | evidence/17-lab-3.png |
| 2026-11-13 | 1.12 | Lab 4: Highly Reliable Database Solution | evidence/18-lab-4.png |
| Total | 46.88 | | |

## Labs

| Evidence file | Lab name | What I built | What failed first and how I fixed it |
|---|---|---|---|
| evidence/15-lab-1.png | Cloud Computing and Storage | Deployed a Discuz! forum architecture by creating image `img-db` from `ecs-db` to launch `ecs-db-img`, provisioning `ecs-web` with an OBS script linked to the database, and mounting the SFS Turbo system `sfs-turbo-test` at `/mnt/sfs_turbo`. | I corrected the `ecs-web` billing to Pay-per-use and the flavor to `s6.large.2` before submitting, and I resubmitted the SFS creation form after missing the first Create Now click, then mounted it with the console command. |
| evidence/16-lab-2.png | High-Performance Network Solution | I deployed a Discuz! network architecture with an RDS database in `subnet-db`, connected `vpc-web` to `vpc-om` via VPC Peering, enabled internet access for `ecs-web` through a NAT gateway with SNAT/DNAT rules, and created the private DNS zone `www.abc.com` resolving to `192.168.1.198`. | When the `ecs-om` terminal disconnected due to a "Client timeout", I reconnected via VNC Login and repeated the ping tests. |
| evidence/17-lab-3.png | Highly Elastic Service System | I deployed an elastic Discuz! service using an RDS database, an ELB with a TCP/80 listener, and the `as-group-web` auto-scaling group (min 1, desired 2, max 5) created from the `image-web` image of `ecs-web`, configured with CPU-based scaling and ELB EIP bandwidth policies. | When creating the auto-scaling configuration, the private image dropdown appeared empty because `image-web` was still building, so I reopened the form once its status changed to Normal. |
| evidence/18-lab-4.png | <Lab name> | <One sentence> | <One sentence> |

## Final exam

- Score: <score>
- Date: <YYYY-MM-DD>
- Evidence: evidence/19-final-exam.png

## Skills summary

<5 to 7 lines in English, first person, naming at least three concrete design techniques from the course.>
