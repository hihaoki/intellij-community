// PROBLEM: Cast will always fail
// FIX: none
fun test(b: Boolean) {
    val x = if (b) "x" else 5
    val y = x <caret>as Double
}