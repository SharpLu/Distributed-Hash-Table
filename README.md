General description
A Distributed Hash Table (DHT) is a distributed storage in a distributed peer-to-peer (P2P) application implemented on top of a structured overlay network. A DHT allows a group of distributed peers to collectively maintain a hash table that can be used to store data objects (or links to the objects) distinguished by some key. Each data object to be stored/fetched/removed is associated with a key, e.g. the name of a person or an integer number. A hash function is applied to the object's key and the resulting hash value is used as an identifier (an index) to select one of hash buckets in the table where the object (or a link to the object) is placed. "Hash buckets" (and so objects stored in them) in a DHT are distributed among peers.

A DHT is implemented on top of a structured overlay network where each node is assigned a unique node identifier. An overlay network is a “virtual” network of nodes (peers) created on top of an existing network, e.g. the Internet. Nodes of the overlay network are exchanges messages using the underlying network. The nodes do not only communicate as sources and destinations but also serve as routers to route messages directed to other nodes.

A DHT can be used as a distributed storage in P2P applications such as distributed file systems, P2P file sharing systems, web caches, etc. Since 2001, when DHTs were first introduced, many different DHT architectures have been proposed, but very few robust implementations have been released.

In this project you are to develop (and evaluate) a k-ary DHT (k >= 2) built on a structured overlay network where nodes should be able to join or to leave the network without losing objects stored in DHT.
Sub-assignment 1: DKS: A structured overlay network
This sub-assignment of the Project 5 aims at design and implementation of a join/leave distributed algorithm to be used for building a structured k-ary P2P (overlay) network [1][2]. You are to  implement join/leave operations that allow  a node (a peer) to join the network: get a contact with node(s), which are already in the network, get a unique ID and build a routing table. 
The major parameters of the network:
N -- the size of the node identifier space (the size of the overlay network). N should be a power of 2.
k -- arity in routing tables [2]
If you choose to implement a network similar to Chord, your implementation must support k >= 2.
Sub-assignment 2: Routing in DKS: implementing the lookup operation
Design and implement a lookup operation in the k-ary overlay network nodeID = lookup (key) as described in the DKS paper [2] or in the Chord paper [1].
Sub-assignment 3: DHT functionality in DKS: implementing get, put and remove operations
Design and implement a (distributed) hash table functionality in the network, i.e. (at least) get/put/remove operations (based on lookup):
object = get(key)
put(key, object)
remove(key)
Design and implementation options
Either DKS or Chord lookup (routing) algorithm
Correction-on-use or periodic correction or combination.
Communication mechanism: either message passing using Java socket API (TCP or/and UDP sockets) or Java RMI.
