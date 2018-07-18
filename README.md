# nchain-bch-toolkit
nChain Bitcoin Cash Toolkit

## Working with addresses

```kotlin

// mainnet cash address to legacy
var address = CashAddress.from("bitcoincash:qqfx3wcg8ts09mt5l3zey06wenapyfqq2qrcyj5x0s")
println(address.toBase58())      // "12gLdGD5q5KdWViDtq3MouheF9PJr8HmB1"
println(address.isMainNet())    // true

// mainnet cash address prefix optional
var address = CashAddress.from("qqfx3wcg8ts09mt5l3zey06wenapyfqq2qrcyj5x0s")
println(address.toCashAddress()) // "bitcoincash:qqfx3wcg8ts09mt5l3zey06wenapyfqq2qrcyj5x0s" 
println(address.isMainNet())    // true


// main net legacy to cash address
var address = CashAddress.from("14krEkSaKoTkbFT9iUCfUYARo4EXA8co6M")
println(address.toCashAddress()) // "bitcoincash:qq5nxh27up6hcm0nn36lxtu7n8a7l6jsj52s8dvtex"
println(address.isMainNet())    // true


// Testnet support
var address = CashAddress.from("bchtest:qph2v4mkxjgdqgmlyjx6njmey0ftrxlnggt9t0a6zy") // with prefix
var address = CashAddress.from("qph2v4mkxjgdqgmlyjx6njmey0ftrxlnggt9t0a6zy") // w/o prefix
var address = CashAddress.from("mqc1tmwY2368LLGktnePzEyPAsgADxbksi") // legacy testnet
println(address.isMainNet())    // false
println(address.isTestNet())    // true

```