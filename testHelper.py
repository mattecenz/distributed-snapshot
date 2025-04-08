import subprocess
import time
import threading
import os
import re

verify = True

local_ip = "192.168.178.21"

log_path = "./logOutput/"


class Task:
    def __init__(self, input_data, port, expected_output):
        self.input = input_data
        self.port = port
        self.expected_output = expected_output
    
    def setLogStatus(self,logClean):
        self.logClean = logClean

tasks = [
    Task(["y", "2000"],2000,{"ciao sono 3","ciao sono 5","ciao sono 7"}),
    Task(["n","3000",local_ip,"2000","/msg",local_ip,"2000","ciao sono 3"],3000,{"ciao sono 5","ciao sono 4","ciao sono 6"}),
    Task(["n","5000",local_ip,"2000","/msg",local_ip,"2000","ciao sono 5","/msg",local_ip,"3000","ciao sono 5"],5000,{"ciao sono 4","ciao sono 7"}),
    Task(["n","4000",local_ip,"2000","/msg",local_ip,"3000","ciao sono 4","/msg",local_ip,"5000","ciao sono 4"],4000,{"ciao sono 7","ciao sono 6"}),
    Task(["n","7000",local_ip,"5000","/msg",local_ip,"5000","ciao sono 7","/msg",local_ip,"2000","ciao sono 7","/msg",local_ip,"4000","ciao sono 7","/exit"],7000,{}),
    Task(["n","6000",local_ip,"2000","/msg",local_ip,"3000","ciao sono 6","/msg",local_ip,"4000","ciao sono 6"],6000,{})
]

task_ready = 0

def open_terminals_with_commands():
    for task in tasks:      
        thread = threading.Thread(target=task_handler, args=("java -jar target/untitled-1.0-SNAPSHOT-jar-with-dependencies.jar",task,verify))  # Add a comma to pass a tuple correctly
        thread.start()
        time.sleep(0.5)  # Ritardo per evitare problemi di apertura simultanea

def msg_received(task, msg):
    if msg in task.expected_output:
        task.expected_output.remove(msg)
    else: 
        print(f"unexpected msg received from  {task.port}: {msg}")

def final_test_check():
    print(f"task ready: {task_ready}\n")
    if(task_ready!=len(tasks)):
        return
    
    test_correct=True
    for task in tasks:
        if(len(task.expected_output)!=0):
            print(f"something went wrong with task {task.port}, {len(task.expected_output)} messages left")
            test_correct=False
        if(not task.logClean):
            print(f"test present severe exception in logs of node: {task.port}")
            #test_correct=False

    if(test_correct):
        print("test successful")

def check_log_for_severe(path, filename):
    full_path = f"{path}/{filename}"
    try:
        with open(full_path, 'r', encoding='utf-8') as log_file:
            for line in log_file:
                if "SEVERE" in line:
                    return False
        return True
    except FileNotFoundError:
        print(f"File non trovato: {full_path}")
        return False


def task_handler(cmd,task, testVerify):
    global task_ready

    # Esegui il comando nel terminale nella directory corrente
    if( not testVerify):
        process = subprocess.Popen(
            ["cmd.exe", "/K", cmd],  # Apri cmd e mantieni aperto
            creationflags=subprocess.CREATE_NEW_CONSOLE,
            stdin=subprocess.PIPE,
            #stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True  # per gestire input/output di testo
        )
    else:
        process = subprocess.Popen(
            ["cmd.exe", "/K", cmd],  # Apri cmd e mantieni aperto
            creationflags=subprocess.CREATE_NEW_CONSOLE,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True  # per gestire input/output di testo
        )

    input = task.input
    # Ciclo per inviare gli input dal nostro array
    for user_input in input:
        time.sleep(2)  # Pausa tra gli input per simulare una sequenza di comandi
        #print(user_input)
        # Scriviamo l'input nel terminale (stdin del processo)
        process.stdin.write(user_input + "\n")
        process.stdin.flush()  # Assicura che l'input venga inviato al programma Java

    print(f"end inputs for task {task.port}")
 
    if(testVerify):
        # output pars
        stdout, stderr = process.communicate()
        print(f"start verify {task.port}")
        
        for line in stdout.split("\n"):
            if "A node has left the network:" in line:
                print(f"Exit message received from {task.port}: {line.strip()}")
            if "ciao sono" in line:
                print(f"Messaggio ricevuto da {task.port}: {line.strip()}")
                match = re.search(r"(ciao sono \d+)", line.strip())
                if match:
                    result = match.group(1)  # Prende solo "ciao sono"
                    msg_received(task, result)

        

        logClean = not check_log_for_severe(log_path, f"DistributedSnapshotLog{task.port}.log")
        print(logClean)
        task.setLogStatus(logClean)
        if(logClean):
            print(f"WARNING: {task.port} has a severe log")

        task_ready += 1
        final_test_check()
    else:
        # Attendi la fine del processo
        stderr = process.communicate()

        # Stampa l'output per il debug
        print("stderr:", stderr)


open_terminals_with_commands()
