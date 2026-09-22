import UIKit

var number1 = 10
var number2 = 20

func swap(a: inout Int, b: inout Int) {
    var temp = a
    a = b
    b = temp
}

swap(a: &number1, b: &number2)
print("after swap - a: \(number1), b: \(number2)")


// Cancellable과 비슷한 상항

class TestClass {
    var mySet: Set<Int> = []
    
    init() {
        print("before inserting: \(mySet)")
        addSet(number: 10, set: &mySet)
        print("after inserting: \(mySet)")
    }
    
    func addSet(number: Int, set: inout Set<Int>) {
        set.insert(number)
    }
}

let testClass = TestClass()
