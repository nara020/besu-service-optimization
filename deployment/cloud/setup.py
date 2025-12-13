#!/usr/bin/env python3
"""
Besu Cloud Network Setup
========================
Generates configuration for deploying Besu nodes across multiple cloud instances.

Paper reference:
"Breaking the Throughput Barrier: 27-Fold Performance Improvement
for Hyperledger Besu Through Service-Layer Optimization"

Usage:
    python setup.py

This script generates:
    - Genesis file with IBFT2/QBFT consensus
    - Node keys for validators
    - Docker Compose files for each node
"""

import os
import json
from pathlib import Path
import sys

try:
    from eth_keys import keys
    from eth_utils import to_checksum_address
except ImportError:
    print("Required packages not found. Install with:")
    print("  pip install eth-keys eth-utils")
    sys.exit(1)

# =============================================================================
# Configuration
# =============================================================================
NUM_VALIDATORS = 4
NUM_TOTAL_NODES = NUM_VALIDATORS + 1  # validators + 1 RPC node
CHAIN_ID = 1337
PROJECT_DIR = Path(__file__).parent.resolve()
CONFIG_DIR = PROJECT_DIR / "config"
DOCKER_COMPOSE_DIR = PROJECT_DIR / "docker-compose"
INITIAL_BALANCE = "0x1000000000000000000000"  # 1,000,000 ETH

# Default node IPs (update these for your environment)
NODE_IPS = [
    "192.168.1.11",  # node1 (validator)
    "192.168.1.12",  # node2 (validator)
    "192.168.1.13",  # node3 (validator)
    "192.168.1.14",  # node4 (validator)
    "192.168.1.15",  # node5 (RPC)
]


def create_genesis_config(consensus="ibft2", block_period=2):
    """Create genesis configuration for Besu network"""

    genesis = {
        "config": {
            "chainId": CHAIN_ID,
            "berlinBlock": 0,
            "londonBlock": 0,
            "shanghaiTime": 0,
            "zeroBaseFee": True,
            consensus: {
                "blockperiodseconds": block_period,
                "epochlength": 30000,
                "requesttimeoutseconds": 4
            }
        },
        "nonce": "0x0",
        "timestamp": "0x58ee40ba",
        "gasLimit": "0x1FFFFFFFFFFFFF",
        "difficulty": "0x1",
        "contractSizeLimit": 99999999999,
        "mixHash": "0x63746963616c2062797a616e74696e65206661756c7420746f6c6572616e6365",
        "coinbase": "0x0000000000000000000000000000000000000000",
        "alloc": {},
        "extraData": "0x"
    }

    return genesis


def generate_validator_keys(num_validators):
    """Generate validator keys and addresses"""
    validators = []

    for i in range(num_validators):
        # Generate random private key
        private_key = keys.PrivateKey(os.urandom(32))
        address = to_checksum_address(private_key.public_key.to_address())

        validators.append({
            "index": i + 1,
            "address": address,
            "private_key": "0x" + private_key.to_bytes().hex(),
            "public_key": "0x" + private_key.public_key.to_bytes().hex()
        })

    return validators


def create_extra_data(validator_addresses):
    """Create IBFT2/QBFT extraData field"""
    # Simplified extraData for IBFT2
    extra_data = "0xf87ea00000000000000000000000000000000000000000000000000000000000000000f854"
    for addr in validator_addresses:
        extra_data += "94" + addr[2:].lower()
    extra_data += "808400000000c0"

    return extra_data


def save_node_files(validators, consensus):
    """Save key files and configuration for each node"""

    # Create config directory
    CONFIG_DIR.mkdir(exist_ok=True)

    # Create genesis with validator allocations
    genesis = create_genesis_config(consensus)

    # Add initial balance to validators
    for v in validators:
        genesis["alloc"][v["address"][2:].lower()] = {"balance": INITIAL_BALANCE}

    # Create extraData
    validator_addresses = [v["address"] for v in validators]
    genesis["extraData"] = create_extra_data(validator_addresses)

    # Save genesis
    genesis_file = CONFIG_DIR / "genesis.json"
    with open(genesis_file, "w") as f:
        json.dump(genesis, f, indent=2)
    print(f"[OK] Genesis file: {genesis_file}")

    # Save keys for each node
    keys_dir = CONFIG_DIR / "keys"
    keys_dir.mkdir(exist_ok=True)

    for v in validators:
        node_dir = keys_dir / f"node{v['index']}"
        node_dir.mkdir(exist_ok=True)

        # Save private key (without 0x prefix for Besu)
        with open(node_dir / "key", "w") as f:
            f.write(v["private_key"][2:])

        # Save public key
        with open(node_dir / "key.pub", "w") as f:
            f.write(v["public_key"])

        # Save address
        with open(node_dir / "address", "w") as f:
            f.write(v["address"])

        # Create enode URL
        enode = f"enode://{v['public_key'][2:]}@{NODE_IPS[v['index']-1]}:30303"
        with open(node_dir / "enode", "w") as f:
            f.write(enode)

        print(f"[OK] Node {v['index']} keys saved")

    return genesis_file


def create_docker_compose_files(validators, consensus):
    """Create individual docker-compose files for each node"""

    DOCKER_COMPOSE_DIR.mkdir(exist_ok=True)

    # Get bootnode enode
    bootnode_pubkey = validators[0]["public_key"][2:]
    bootnode_enode = f"enode://{bootnode_pubkey}@{NODE_IPS[0]}:30303"

    consensus_api = "IBFT" if consensus == "ibft2" else "QBFT"

    for v in validators:
        i = v["index"]
        node_ip = NODE_IPS[i-1]

        bootnode_line = "" if i == 1 else f'      - "--bootnodes={bootnode_enode}"\n'

        content = f'''# Besu Node {i} - Validator
# Deploy to: {node_ip}

services:
  node{i}:
    image: hyperledger/besu:24.1.1
    container_name: besu-node{i}
    network_mode: host
    environment:
      - BESU_OPTS=-XX:MaxRAMPercentage=80.0 -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+ParallelRefProcEnabled
    volumes:
      - ./data:/data
      - ../config/genesis.json:/config/genesis.json
      - ../config/keys/node{i}/key:/config/key
    command:
      - "--data-path=/data"
      - "--genesis-file=/config/genesis.json"
      - "--node-private-key-file=/config/key"
      - "--rpc-http-enabled"
      - "--rpc-http-api=ETH,NET,WEB3,ADMIN,TXPOOL,{consensus_api}"
      - "--rpc-http-host=0.0.0.0"
      - "--rpc-http-port=8545"
      - "--rpc-http-cors-origins=*"
      - "--rpc-http-max-active-connections=50000"
      - "--host-allowlist=*"
      - "--p2p-host={node_ip}"
      - "--p2p-port=30303"
      - "--min-gas-price=0"
      - "--profile=enterprise"
      - "--data-storage-format=BONSAI"
      - "--bonsai-limit-trie-logs-enabled=false"
      - "--tx-pool=layered"
      - "--tx-pool-layer-max-capacity=5000000"
{bootnode_line}    restart: unless-stopped
'''

        compose_file = DOCKER_COMPOSE_DIR / f"node{i}.yml"
        with open(compose_file, "w") as f:
            f.write(content)
        print(f"[OK] Docker Compose: {compose_file}")

    # RPC node
    rpc_content = f'''# Besu Node {NUM_TOTAL_NODES} - RPC Only
# Deploy to: {NODE_IPS[NUM_TOTAL_NODES-1]}

services:
  node{NUM_TOTAL_NODES}:
    image: hyperledger/besu:24.1.1
    container_name: besu-node{NUM_TOTAL_NODES}-rpc
    network_mode: host
    environment:
      - BESU_OPTS=-XX:MaxRAMPercentage=80.0 -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+ParallelRefProcEnabled
    volumes:
      - ./data:/data
      - ../config/genesis.json:/config/genesis.json
    command:
      - "--data-path=/data"
      - "--genesis-file=/config/genesis.json"
      - "--rpc-http-enabled"
      - "--rpc-http-api=ETH,NET,WEB3,ADMIN,TXPOOL,{consensus_api}"
      - "--rpc-http-host=0.0.0.0"
      - "--rpc-http-port=8545"
      - "--rpc-http-cors-origins=*"
      - "--rpc-http-max-active-connections=100000"
      - "--host-allowlist=*"
      - "--p2p-host={NODE_IPS[NUM_TOTAL_NODES-1]}"
      - "--p2p-port=30303"
      - "--bootnodes={bootnode_enode}"
      - "--min-gas-price=0"
      - "--profile=enterprise"
      - "--data-storage-format=BONSAI"
      - "--tx-pool=layered"
      - "--miner-enabled=false"
    restart: unless-stopped
'''

    compose_file = DOCKER_COMPOSE_DIR / f"node{NUM_TOTAL_NODES}.yml"
    with open(compose_file, "w") as f:
        f.write(rpc_content)
    print(f"[OK] Docker Compose (RPC): {compose_file}")


def save_validators_json(validators):
    """Save validators.json for reference"""
    validators_file = PROJECT_DIR / "validators.json"
    with open(validators_file, "w") as f:
        json.dump(validators, f, indent=2)
    print(f"[OK] Validators file: {validators_file}")


def print_deployment_guide():
    """Print deployment instructions"""
    print("\n" + "=" * 60)
    print("DEPLOYMENT GUIDE")
    print("=" * 60)

    print("\n1. Update NODE_IPS in this script with your server IPs")
    print("\n2. Copy files to each server:")
    for i in range(1, NUM_TOTAL_NODES + 1):
        print(f"   Server {i} ({NODE_IPS[i-1]}):")
        print(f"     - config/genesis.json")
        print(f"     - config/keys/node{i}/")
        print(f"     - docker-compose/node{i}.yml")

    print("\n3. Start each node:")
    print("   docker compose -f node1.yml up -d")

    print("\n4. Check node status:")
    print("   curl -X POST http://localhost:8545 \\")
    print("     -H 'Content-Type: application/json' \\")
    print("     -d '{\"jsonrpc\":\"2.0\",\"method\":\"eth_blockNumber\",\"params\":[],\"id\":1}'")

    print("\n" + "=" * 60)


def main():
    print("\n=== Besu Cloud Network Setup ===\n")

    # Select consensus
    print("Select consensus:")
    print("  1. IBFT2 (default)")
    print("  2. QBFT")
    choice = input("Choice [1]: ").strip() or "1"
    consensus = "ibft2" if choice == "1" else "qbft"

    # Block period
    print("\nBlock period (seconds):")
    print("  1. 1s (testing)")
    print("  2. 2s (default)")
    print("  3. 5s (stable)")
    choice = input("Choice [2]: ").strip() or "2"
    block_period = {1: 1, 2: 2, 3: 5}.get(int(choice), 2)

    print(f"\n[INFO] Generating {NUM_VALIDATORS} validator keys...")
    validators = generate_validator_keys(NUM_VALIDATORS)

    print(f"[INFO] Creating genesis file ({consensus}, {block_period}s blocks)...")
    save_node_files(validators, consensus)

    print(f"[INFO] Creating docker-compose files...")
    create_docker_compose_files(validators, consensus)

    print(f"[INFO] Saving validators.json...")
    save_validators_json(validators)

    print_deployment_guide()

    print("\n[DONE] Setup complete!")


if __name__ == "__main__":
    main()
