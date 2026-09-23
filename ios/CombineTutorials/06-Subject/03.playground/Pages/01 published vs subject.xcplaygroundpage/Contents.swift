import UIKit
import Combine

class PublishedClass {
    @Published var value = 0
}

let publishedClass = PublishedClass()

let publishedSubscription = publishedClass.$value
    .sink { value in
        print("Published value: \(value)")
    }

publishedClass.value = 1
publishedClass.value = 2
publishedClass.value = 3

print("published value \(publishedClass.value)")

print("==============")

class SubjectClass{
    var subject = CurrentValueSubject<Int, Never>(0)
}

let subjectClass = SubjectClass()

let subjectSubscription = subjectClass.subject
    .sink { number in
        print("Subject value: \(number)")
    }

subjectClass.subject.send(1)
subjectClass.subject.send(2)
subjectClass.subject.send(3)

print("subject value: \(subjectClass.subject.value)")
