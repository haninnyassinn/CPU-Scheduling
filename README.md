# 💻 CPU Scheduling Simulator 

## 📌 Overview

This project is a **Java-based CPU Scheduling Simulator** that demonstrates the behavior of different CPU scheduling algorithms used in operating systems.

The simulator allows the user to enter process information and execute the following scheduling algorithms:

✅ **Preemptive Shortest Job First (SJF) Scheduling with Context Switching**
✅ **Round Robin (RR) Scheduling with Context Switching**
✅ **Preemptive Priority Scheduling with Starvation Solution**
✅ **AG Scheduling**

The program calculates and displays:

* 📋 Execution order of processes
* ⏳ Waiting Time for each process
* 🔄 Turnaround Time for each process
* 📊 Average Waiting Time
* 📈 Average Turnaround Time
* 🧠 Quantum history updates for AG Scheduling

---

# 🚀 Features

## 1️⃣ Preemptive Shortest Job First (SJF)

* Executes the process with the shortest remaining burst time.
* Supports context switching time.
* Preempts currently running processes when a shorter job arrives.

---

## 2️⃣ Round Robin (RR)

* Each process gets a fixed time quantum.
* Processes are executed cyclically.
* Context switching time is included.

---

# 3️⃣ Preemptive Priority Scheduling

* Processes are scheduled based on priority.
* Lower priority number means higher priority.
* Includes starvation prevention using aging.

## 🛡️ Starvation Solution

Aging technique is applied:

* Waiting processes gradually increase in priority over time.
* Prevents low-priority processes from waiting indefinitely.

---

# 4️⃣ AG Scheduling

## 🧠 Description

AG Scheduling is a hybrid scheduling algorithm combining:

* 🔹 FCFS
* 🔹 Non-preemptive Priority
* 🔹 Preemptive SJF

Each process has:

* ⏰ Arrival Time
* ⚡ Burst Time
* 🏷️ Priority
* 🔄 Quantum Time

---

# ⚙️ AG Scheduling Phases

## 🟡 Phase 1 – FCFS

The process runs using FCFS until:

```text id="x9f8k2"
ceil(25% of Quantum)
```

## 🟠 Phase 2 – Non-preemptive Priority

The process continues using Priority Scheduling until:

```text id="w3d7t1"
ceil(50% of Quantum)
```

## 🔴 Phase 3 – Preemptive SJF

The remaining quantum is executed using Preemptive SJF.

---

# 📥 Program Input

The user must enter:

## 🧾 General Input

* 🔢 Number of Processes
* ⏱️ Round Robin Time Quantum
* 🔄 Context Switching Time

## 🧩 For Each Process

* 🏷️ Process Name
* ⏰ Arrival Time
* ⚡ Burst Time
* 📌 Priority

For AG Scheduling:

* 🔄 Initial Quantum

---

# 📤 Program Output

For each scheduling algorithm, the program outputs:

## 📋 Execution Order

Example:

```text id="z2j4f6"
P1 → P2 → P3 → P1
```

## ⏳ Waiting Time

Waiting time for each process.

## 🔄 Turnaround Time

Turnaround time for each process.

## 📊 Average Waiting Time

## 📈 Average Turnaround Time

## 🧠 AG Scheduling Output

* Full quantum update history
* Execution timeline

---

# 🎯 Learning Outcomes

Through this assignment:

* 🧠 Understand CPU scheduling algorithms
* ⚖️ Compare scheduling performance
* 📊 Analyze waiting and turnaround times
* 🛡️ Learn starvation handling
* 🔀 Implement hybrid scheduling algorithms
* 💡 Practice object-oriented programming in Java

---

