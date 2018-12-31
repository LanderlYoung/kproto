package kn.workaround

/**
 * <pre>
 * Author: landerlyoung@gmail.com
 * Date:   2018-12-31
 * Time:   20:00
 * Life with Passion, Code with Creativity.
 * </pre>
 */

actual fun String._format(vararg args: Any) = this.format(args = *args)
