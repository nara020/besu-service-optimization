// SPDX-License-Identifier: MIT
pragma solidity 0.8.24;

/**
 * @title NativeTokenFaucetV1
 * @notice Simple faucet contract for initial account funding in private Besu networks
 * @dev Used in the paper "Breaking the Throughput Barrier: 27-Fold Performance
 *      Improvement for Hyperledger Besu Through Service-Layer Optimization"
 *
 * Purpose:
 * - Provides initial ETH funding (1 ETH) for new accounts in zero-fee private networks
 * - Each account can only claim once (anti-sybil protection)
 * - Called by the Node.js middleware (Layer 2) during account registration
 *
 * Usage:
 * 1. Deploy contract and fund it with ETH
 * 2. Call initialize() to set the owner
 * 3. New accounts call open() to receive 1 ETH
 */
contract NativeTokenFaucetV1 {
    address public owner;
    mapping(address => bool) public opened;
    uint256 public constant FAUCET_AMOUNT = 1 ether;

    bool public initialized;
    bool private locked;

    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner can call this function");
        _;
    }

    modifier nonReentrant() {
        require(!locked, "ReentrancyGuard: reentrant call");
        locked = true;
        _;
        locked = false;
    }

    /**
     * @notice Initialize the contract (call once after deployment)
     */
    function initialize() external {
        require(!initialized, "Already initialized");
        owner = msg.sender;
        initialized = true;
    }

    /**
     * @notice Allow contract to receive ETH
     */
    receive() external payable {}

    /**
     * @notice Claim initial funding (1 ETH)
     * @dev Each address can only call this once
     *
     * Paper context:
     * This function is called by the InitialFundingAdapter in the Node.js
     * middleware. The ~4 second wait for transaction finality happens in
     * the middleware layer, NOT while holding a Java backend DB connection.
     */
    function open() external nonReentrant {
        require(!opened[msg.sender], "Already opened");
        require(address(this).balance >= FAUCET_AMOUNT, "Insufficient contract balance");

        opened[msg.sender] = true;
        (bool success, ) = payable(msg.sender).call{value: FAUCET_AMOUNT}("");
        require(success, "Transfer failed");
    }

    /**
     * @notice Check ETH balance of any account
     */
    function query(address account) external view returns (uint256) {
        return account.balance;
    }

    /**
     * @notice Check faucet contract balance
     */
    function contractBalance() external view returns (uint256) {
        return address(this).balance;
    }

    /**
     * @notice Deposit ETH to the faucet (anyone can call)
     */
    function deposit() external payable {}

    /**
     * @notice Withdraw specific amount (owner only)
     */
    function withdraw(uint256 amount) external onlyOwner {
        require(address(this).balance >= amount, "Insufficient balance");
        (bool success, ) = payable(owner).call{value: amount}("");
        require(success, "Withdraw failed");
    }

    /**
     * @notice Withdraw all ETH (owner only)
     */
    function withdrawAll() external onlyOwner {
        uint256 balance = address(this).balance;
        (bool success, ) = payable(owner).call{value: balance}("");
        require(success, "Withdraw failed");
    }

    /**
     * @notice Get contract version
     */
    function version() external pure returns (string memory) {
        return "1.0.0";
    }
}
