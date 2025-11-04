package com.dp.jumpster.util

/**
 * 简单计算器工具类，支持加减法表达式计算
 */
object SimpleCalculator {

    /**
     * 计算简单的加减法表达式
     * 支持格式如: "123+45-67"
     * @param expression 表达式字符串
     * @return 计算结果，如果表达式无效则返回null
     */
    fun calculate(expression: String): Int? {
        try {
            // 移除所有空格
            val cleanExpression = expression.replace(" ", "")
            
            // 如果是纯数字，直接返回
            if (cleanExpression.matches(Regex("^\\d+$"))) {
                return cleanExpression.toIntOrNull()
            }
            
            // 检查表达式格式是否有效
            if (!isValidExpression(cleanExpression)) {
                return null
            }
            
            // 分割表达式
            val numbers = cleanExpression.split(Regex("[+\\-]")).filter { it.isNotEmpty() }
            val operators = cleanExpression.filter { it == '+' || it == '-' }.toList()
            
            // 处理第一个数字可能是负数的情况
            var result = if (cleanExpression.startsWith("-")) {
                -numbers[0].toInt()
            } else {
                numbers[0].toInt()
            }
            
            // 计算结果
            var opIndex = if (cleanExpression.startsWith("-")) 1 else 0
            for (i in 1 until numbers.size) {
                if (opIndex >= operators.size) break
                
                val num = numbers[i].toInt()
                when (operators[opIndex]) {
                    '+' -> result += num
                    '-' -> result -= num
                }
                opIndex++
            }
            
            return result
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * 检查表达式格式是否有效
     */
    private fun isValidExpression(expression: String): Boolean {
        // 只允许数字、加号和减号
        if (!expression.matches(Regex("^-?\\d+([+\\-]\\d+)*$"))) {
            return false
        }
        
        // 不允许连续的运算符
        if (expression.contains("++") || expression.contains("--") || 
            expression.contains("+-") || expression.contains("-+")) {
            return false
        }
        
        return true
    }
}
