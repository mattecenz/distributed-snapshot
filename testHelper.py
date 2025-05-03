import subprocess
import time
import threading
import os
import glob
import re
import json
import sys

import javaobj

verify = True
print_ns_rt = True

task_ready = 0
jar_comand = "java -jar appExample-module/target/dapplication-1.0-SNAPSHOT-jar-with-dependencies.jar"

#local_ip = "10.169.155.239"

log_path = "./logOutput/"
snapshot_path = "./snapshots/"

snapshot_creators = []

class Task:
    def __init__(self, input_data, port, expected_output, sync_points=None, expected_final_father = '0', expected_final_rt_entries = [], expected_children=[]):
        self.input = input_data
        self.port = port
        self.expected_output = expected_output
        self.sync_points = sync_points or []  # Lista di indici di sincronizzazione
        self.logClean = None
        self.receivedSnapshotCount = 0
        self.expected_final_father = expected_final_father
        self.expected_final_rt_entries = expected_final_rt_entries
        self.expected_children = expected_children

    @staticmethod
    def from_dict(data, local_ip):
        input_data = replace_tokens(data['input'], local_ip, data['port'])
        expected_output = replace_tokens(data['expected_output'], local_ip, data['port'])
        
        return Task(
            input_data=input_data,
            port=data['port'],
            expected_output=expected_output,
            sync_points=data.get('sync_points', []),
            expected_final_father=data.get('expected_final_father', '0'),
            expected_final_rt_entries=data.get('expected_final_rt_entries', []),
            expected_children=data.get('expected_children', [])
        )

    def receiveNewSnapshot(self):
        self.receivedSnapshotCount += 1

    def setLogStatus(self, logClean):
        self.logClean = logClean

def replace_tokens(string_list, local_ip, port):
    """
    Replace tokens <local_ip> and <exit-notify-received (port)> in a list of string.
    """
    updated_data = []
    for item in string_list:
        if "<snapshot_start>" in item:
            item = item.replace("<snapshot_start>", "/snapshot")
            snapshot_creators.append(port)
        if "<local_ip>" in item:
            item = item.replace("<local_ip>", local_ip)
        # Sostituzione del token <exit-notify-received (port)>
        match = re.search(r"<exit-notify-received \((\d+)\)>", item)
        if match:
            port = match.group(1)  # Estrae la porta dal token
            item = item.replace(match.group(0), f"{local_ip}:{port}")  # Sostituisce il token con local_ip:por
        updated_data.append(item)
    return updated_data

def open_terminals_with_commands(tasks):
    sync_lengths = [len(task.sync_points) for task in tasks]
    if len(set(sync_lengths)) != 1:
        print("Error: The tasks do not have the same number of synchronization points!")
        print("Lengths found:", sync_lengths)
        return  # Interrompi l'esecuzione se non sono coerenti

    for task in tasks:      
        thread = threading.Thread(target=task_handler, args=(jar_comand,task,verify))  # Add a comma to pass a tuple correctly
        thread.start()
        time.sleep(0.5)  # Ritardo per evitare problemi di apertura simultanea

def msg_received(task, msg):
    if msg in task.expected_output:
        task.expected_output.remove(msg)
    else: 
        print(f"unexpected msg received from  {task.port}: {msg}")

def load_tasks_from_file(file_path, local_ip):
    with open(file_path, 'r') as f:
        tasks_data = json.load(f)
    return [Task.from_dict(task_data, local_ip) for task_data in tasks_data]

def snapshot_remove(snapshot_path):
    bin_files = glob.glob(os.path.join(snapshot_path, "*.bin"))
    for file_path in bin_files:
        try:
            os.remove(file_path)
        except Exception as e:
            print(f"Error deleting {file_path}: {e}")

def snapshot_exists(ip, port1, port2, extension=".bin"):
    pattern = re.compile(rf".*-{re.escape(ip)}-{port1}-{port2}_.*\{extension}$")
    directory = snapshot_path
    
    if not os.path.isdir(directory):
        print("Directory snapshots non trovata!")
        return False

    for filename in os.listdir(directory):
        if pattern.match(filename):
            #print(f"Trovato: {filename}")
            return True, directory+filename

    print(f" file {port1}-{port2} non trovato trovato.")
    return False

def read_snapshot(file_path, task):
    with open(file_path, 'rb') as f:
        obj = javaobj.load(f)

        # Verifica se l'oggetto è di tipo 'JavaObject' o qualcosa di diverso
        if isinstance(obj, javaobj.JavaObject):
            try:
                if hasattr(obj, 'serializableSpanningTree'):
                    serializable_spanning_tree = obj.serializableSpanningTree
                    if not pars_spt(serializable_spanning_tree,task): return False
                else: 
                    print('no spanning tree saved in the snapshot')
                    return False
                if hasattr(obj, 'routingTable'):
                    routing_table = obj.routingTable
                    if not snapshot_verify_rt(routing_table, task): return False
                else:
                    print('no routing table present in the snapshot')
                    return False
                if hasattr(obj,'applicationState') and hasattr(obj,'messageInputStack'):
                    application_state = obj.applicationState
                    message_input_stack = obj.messageInputStack
                    snapshot_cut_consistency_verification(application_state, message_input_stack, task)
                else:
                    print('no application state or message input stack present in the snapshot')
                    return False
            
            except Exception as e:
                print(f"Error while parsing java obj: {e}")
                return False
        else:
            print("the snapshot isn't a 'JavaObject'.")
            return False
    return True

def snapshot_cut_consistency_verification(application_state, message_input_stack, task):
    #print(f"{application_state} \n{message_input_stack} ")

    messages = []
    messages.extend(parse_application_state(application_state, task))
    messages.extend(parse_message_input_stack(message_input_stack, task))
    
    if print_ns_rt: print(messages)

def parse_application_state(application_state, task):
    if isinstance(application_state, javaobj.JavaObject):
        try:
            if hasattr(application_state, 'messageHistory'):
                messageHistory = application_state.messageHistory
                return messageHistory
        except Exception as e:
            print(f"Error while parsing java obj: {e}")
            return []
    else:
        print(f"the application_state of {task.port} isn't a 'JavaObject'.")  
        return []
    return []

def parse_message_input_stack(message_input_stack, task):
    if isinstance(message_input_stack, javaobj.JavaObject):
        try:
            if hasattr(message_input_stack, 'elementData'):
                stack_elements = message_input_stack.elementData
                return [e for e in stack_elements if e is not None]
            else:
                print(f"'elementData' attribute not found in message_input_stack for task {task.port}.")
                return []
        except Exception as e:
            print(f"Error while parsing stack for task {task.port}: {e}")
            return []
    else:
        print(f"The message_input_stack of {task.port} isn't a 'JavaObject'.")  

def snapshot_verify_rt(routing_table, task):
    destinations = []
    nexthops = []

    # parse java obj
    if isinstance(routing_table, javaobj.JavaObject):
        try:
            if hasattr(routing_table, 'routingTableFields'):
                table = routing_table.routingTableFields
                if isinstance(table, javaobj.JavaObject) and hasattr(table, '__dict__'):
                    annotations = table.__dict__.get('annotations', None)
                    if annotations and isinstance(annotations, list):
                        # Filtra eventuali header non oggetto
                        clean_annotations = [a for a in annotations if isinstance(a, javaobj.JavaObject)]
                        
                        if len(clean_annotations) % 2 != 0:
                            print(f"Warning: annotations list is not even! ({len(clean_annotations)} items)")
                        
                        for i in range(0, len(clean_annotations), 2):
                            destination = clean_annotations[i]
                            tmp = clean_annotations[i + 1] if i + 1 < len(clean_annotations) else None
                            if isinstance(tmp, javaobj.JavaObject):
                                if hasattr(tmp, 'nodeName'):
                                    nexthop = tmp.nodeName
                                    nexthops.append(nexthop)

                            destinations.append(destination)
                            
                        
                        if print_ns_rt: snapshot_print_rt(destinations,nexthops,task)
        except Exception as e:
            print(f"Error while parsing java obj: {e}")
            return False
    else:
        print(f"the routing_table of {task.port} isn't a 'JavaObject'.")  
        return False

    # rt verification
    destionation_ports = []
    for destination in destinations:
        b, IP, port = nodeName_pars(destination)
        if b:
            destionation_ports.append(port)

    for entri in task.expected_final_rt_entries:
        if(not entri in destionation_ports): 
            print(f"missing  entri {entri} in rt of {task.port}")
            return False
        
    return True
   
def snapshot_print_rt(keys,values,task):
    print ("\n")
    print(f"###################### start rt of {task.port}  ###################### ")
    for key, value in zip(keys, values):
        kb,kIP,kport = nodeName_pars(key)
        vb,vIP,vport = nodeName_pars(value)
        if kb and vb:
            print(f"{kIP}:{kport} -> {vIP}:{vport}")
    print(f"###################### end rt of {task.port}  ###################### ")
    print ("\n")
            
def nodeName_pars(node_name):
    if isinstance(node_name, javaobj.JavaObject):
        try:
            if hasattr(node_name, 'IP') and hasattr(node_name, 'port'):
                IP = node_name.IP
                port = node_name.port
                return True,IP,port
        except Exception as e:
            print(f"Error while parsing java obj: {e}")
    return False,None,None

def pars_spt(spt,task):
    if isinstance(spt, javaobj.JavaObject):
        try:
            if hasattr(spt, 'anchorNodeName'):
                anchor_node = spt.anchorNodeName
                if not snapshot_verify_anchor(anchor_node,task): return False
            else: 
                print('no anchor node saved in the snapshot')
                return False
            if hasattr(spt, 'childrenNames'):
                children_names = spt.childrenNames
                if not snapshot_verify_children(children_names,task): return False
            else: 
                print('no childrenNames saved in the snapshot')
                return False
        except Exception as e:
            print(f"Error while parsing java obj: {e}")
            return False
    else:
        print("the spt isn't a 'JavaObject'.")
        return False
    return True

def snapshot_verify_children(children_names, task):
    if isinstance(children_names, javaobj.JavaObject):
        try:
            # Un ArrayList serializzato ha 'annotations' come lista dei suoi elementi
            if hasattr(children_names, 'annotations'):
                elements = children_names.annotations
                for element in elements:
                    if isinstance(element, javaobj.JavaObject): 
                        b,ip,port = nodeName_pars(element)
                        if not b: 
                            print(f"the element is not a valid node name {element}")
                            return False
                        if not port in task.expected_children:
                            print(f"unexpected child {port} for node {task.port}")
                            return False
                        else:
                            task.expected_children.remove(port)
                if not len(task.expected_children) == 0:
                    print(f"missing children for node {task.port} {task.expected_children}")
            else:
                print(f"No 'annotations' field found in children_names of {task.port}")
                return False
        except Exception as e:
            print(f"Error parsing children_names for {task.port}: {e}")
            return False
    else:
        print(f"children_names of {task.port} is not a JavaObject.")
        return False
    return True


def snapshot_verify_anchor(anchor_node, task):
    if(task.expected_final_father==0): return True

    b, IP, port = nodeName_pars(anchor_node)
    if b:
        if(print_ns_rt): 
            print(f"the father of {task.port} is {IP}:{port}")
        if((not task.expected_final_father==0) and (not task.expected_final_father==port)):
             print(f"wrong father saved in the snapshot file of {task.port}")
             return False
    return b
    
def final_test_check():
    print(f"task ready: {task_ready}\n")
    if(task_ready!=len(tasks)):
        return
    
    test_correct=True
    for task in tasks:
        if(len(task.expected_output)!=0):
            print(f"something went wrong with task {task.port}, {len(task.expected_output)} messages left: {task.expected_output}")
            test_correct=False
        
        for creator in snapshot_creators:
            b, path = snapshot_exists(local_ip, creator, task.port)
            if(not b): test_correct = False
            elif not read_snapshot(path, task): test_correct = False

        if(not task.logClean):
            print(f"test present severe exception in logs of node: {task.port}")
            #test_correct=False
        if(not task.receivedSnapshotCount == len(snapshot_creators)):
            print(f"missing snapshot I/O message in node: {task.port}")
            test_correct=False
    
    print("\n")
    if(test_correct):
        print("test successful!")
    else: 
        print("SEVERE: test fail!!!!")

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
    for i, user_input in enumerate(input):
        time.sleep(2)
        #print(f"node: {task.port} input {user_input}")
        process.stdin.write(user_input + "\n")
        process.stdin.flush()

        if i in task.sync_points:
            print(f"Task {task.port} reached sync point {i}, waiting...")
            try:
                sync_barrier.wait()
                print(f"Task {task.port} passed sync point {i}.")
                task.sync_points.remove(i)  # Rimuovi il punto una volta sincronizzato
                time.sleep(2)
            
            except threading.BrokenBarrierError:
                print(f"Barrier broken for task {task.port} at point {i}")

    print(f"end inputs for task {task.port}")
 
    if(testVerify):
        # output pars
        stdout, stderr = process.communicate()
        print(f"start verify {task.port}")
        
        for line in stdout.split("\n"):
            if "Collecting state of the application." in line:
                print(f"snapshot message receive on node {task.port}")
                task.receiveNewSnapshot()
            if "A node has left the network:" in line:
                #A node has left the network: 10.189.83.22:7000
                match = re.search(r"(\d+\.\d+\.\d+\.\d+:\d+)", line.strip())
                if match:
                    result = match.group(1)
                    print(f"Exit message received from {task.port}: {result}")
                    msg_received(task, result)
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

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python script.py <path_to_tasks_file> <local_ip>")
        sys.exit(1)
    
    snapshot_remove(snapshot_path)

    tasks_file_path = sys.argv[1]
    local_ip = sys.argv[2]  # Get the local_ip from the command line argument
    tasks = load_tasks_from_file(tasks_file_path, local_ip)
    sync_barrier = threading.Barrier(len(tasks))

    open_terminals_with_commands(tasks)