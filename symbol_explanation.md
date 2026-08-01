# ScoreMe MSME Credit Pipeline Scheduling Problem
## Mathematical Symbol Reference & Formulation Guide

This document provides a comprehensive, symbol-by-symbol breakdown of all mathematical notation, formulas, variables, and objective function terms used in the **MSME Credit Pipeline Scheduling Solution**.

---

## 1. Objective & Penalty Function Symbols

$$P(\sigma) = P_{\text{base}}(\sigma) + \alpha \cdot P_{\text{imbalance}}(\sigma) + \beta \cdot P_{\text{sla\_risk}}(\sigma)$$

| Symbol | Name | Description | Plain English Meaning |
| :--- | :--- | :--- | :--- |
| **$P(\sigma)$** | Total Penalty Score | The overall objective function value to be minimized. | The overall "Badness Score" of the schedule. Lower is better. |
| **$\sigma$** | Assignment Function | Mapping function $\sigma: T \to [K]$ assigning task $t_i$ to slot $s$. | The final schedule decision vector showing which slot every task runs in. |
| **$P_{\text{base}}(\sigma)$** | Base Delay Cost | $\sum_{i=1}^n w(t_i) \cdot \sigma(t_i)$ | Penalty incurred by delaying task execution to later slots. |
| **$P_{\text{imbalance}}(\sigma)$** | Load Imbalance Variance | Variance of resource utilization across slots relative to mean $\bar{U}_m$. | Penalty incurred when some slots are overloaded while others sit idle. |
| **$P_{\text{sla\_risk}}(\sigma)$** | SLA Boundary Risk | $\sum_{i=1}^n w(t_i) \cdot \left( \frac{\sigma(t_i) - \ell_i}{u_i - \ell_i + \epsilon} \right)^2$ | Exponential penalty incurred when scheduling tasks dangerously close to deadline $u_i$. |
| **$\alpha$** | Load Imbalance Weight | Tuning hyperparameter constant (default = $5.0$). | Multiplier controlling how strongly we penalize server load imbalance. |
| **$\beta$** | SLA Risk Weight | Tuning hyperparameter constant (default = $2.0$). | Multiplier controlling how strongly we penalize scheduling near deadline boundaries. |

---

## 2. Task & Pipeline Notation

| Symbol | Name | Description | Example / Plain English Meaning |
| :--- | :--- | :--- | :--- |
| **$n$** | Task Count | Total number of credit pipeline tasks submitted. | Total tasks in batch (e.g., $n = 50$). |
| **$i$** | Task Index | Loop index variable $i \in \{1, 2, \dots, n\}$. | Counter identifying a specific task. |
| **$t_i$** | Task Entity | The $i$-th specific pipeline task. | Task object $t_1 = \text{OCR Bank Statement}$, $t_2 = \text{Bureau Pull}$. |
| **$w(t_i)$** / **$w_i$** | Priority Weight | Priority importance weight of task $t_i$. | $w_i = 10.0$ for Tier-1 PSU Bank, $w_i = 1.0$ for minor check. |
| **$\sigma(t_i)$** | Assigned Slot | The discrete slot number assigned to task $t_i$. | E.g., $\sigma(t_1) = 2$ means task $t_1$ runs in Slot 2. |
| **$\sum$** | Summation | Capital Sigma summation operator. | Sum up / add together all elements across the index. |

---

## 3. Slot & Cluster Resource Notation

| Symbol | Name | Description | Example / Plain English Meaning |
| :--- | :--- | :--- | :--- |
| **$K$** | Total Slots | Total available processing time windows. | Total slots in execution cycle (e.g., $K = 8$). |
| **$s$** | Slot Index | Discrete slot index $s \in \{1, 2, \dots, K\}$. | Counter identifying a specific slot window. |
| **$d$** | Resource Dimensions | Number of tracked cluster resource types ($d = 4$). | 4 Dimensions: $\text{CPU}, \text{RAM}, \text{GPU}, \text{Network}$. |
| **$m$** | Dimension Index | Resource dimension index $m \in \{1, 2, 3, 4\}$. | $1 \to \text{CPU}$, $2 \to \text{RAM}$, $3 \to \text{GPU}$, $4 \to \text{Network}$. |
| **$r_m(t_i)$** | Resource Demand | Quantity of resource $m$ requested by task $t_i$. | E.g., $r_{\text{CPU}}(t_1) = 8$ CPU cores. |
| **$C_m(s)$** | Slot Capacity | Maximum capacity vector of slot $s$ for resource $m$. | E.g., $C_{\text{CPU}}(s) = 32$ CPU cores max in Slot $s$. |
| **$\sum_{i: \sigma(t_i)=s} r_m(t_i)$** | Total Slot Demand | Sum of resource $m$ demanded by all tasks placed in slot $s$. | Combined resource load active during slot $s$. |
| **$\frac{\sum r_m(t_i)}{C_m(s)}$** | Slot Utilization | Fractional resource load of slot $s$ in dimension $m$. | E.g., $0.80$ means slot $s$ is 80% full. |
| **$\bar{U}_m$** | Average Utilization | Mean utilization of resource $m$ across all $K$ slots. | Average load baseline across the entire cluster. |

---

## 4. SLA Time Window & Risk Notation

| Symbol | Name | Description | Example / Plain English Meaning |
| :--- | :--- | :--- | :--- |
| **$\ell_i$** | SLA Lower Bound | Earliest allowed slot index for task $t_i$. | Start of window (e.g., Slot 1). |
| **$u_i$** | SLA Upper Bound | Latest allowed slot index (deadline) for task $t_i$. | Hard deadline boundary (e.g., Slot 4). |
| **$[\ell_i, u_i]$** | SLA Window | Valid temporal execution interval for task $t_i$. | Task $t_i$ must run in slot $s$ where $\ell_i \le s \le u_i$. |
| **$\frac{\sigma(t_i) - \ell_i}{u_i - \ell_i + \epsilon}$** | Normalized Offset Ratio | Fractional distance of assigned slot within window $[\ell_i, u_i]$. | $0.0$ at start window, $1.0$ at deadline. |
| **$\epsilon$** | Epsilon Constant | Small numerical constant ($10^{-5}$). | Prevents division-by-zero when $\ell_i = u_i$. |

---

## 5. Constraint Notation

| Constraint | Name | Formal Expression | Plain English Meaning |
| :--- | :--- | :--- | :--- |
| **F1** | Conflict Avoidance | $\forall (t_i, t_j) \in E : \sigma(t_i) \neq \sigma(t_j)$ | Conflicting tasks (sharing GPU bus or Kafka key) cannot run in the same slot. |
| **F2** | Capacity Bounds | $\forall s \in [K], \forall m \in [d] : \sum_{i: \sigma(t_i)=s} r_m(t_i) \le C_m(s)$ | Total task demand in any slot cannot exceed slot capacity. |
| **F3** | SLA Compliance | $\forall t_i \in T : \ell_i \le \sigma(t_i) \le u_i$ | Every task must be scheduled inside its allowed time window. |

---
*End of Symbol Reference Guide.*
