package kn.workaround

/**
 * <pre>
 * Author: landerlyoung@gmail.com
 * Date:   2018-12-31
 * Time:   20:00
 * Life with Passion, Code with Creativity.
 * </pre>
 */


// fix kotlin bug
fun Char.isDigit(): Boolean = this.toInt() in ('0'.toInt()..'9'.toInt())

expect fun String._format(vararg args: Any): String
