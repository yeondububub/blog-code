import Combine
import Foundation
import PlaygroundSupport

PlaygroundPage.current.needsIndefiniteExecution = true

//Convinience Publishers

//Sequence Publishers, Just, Empty, Fail, Future

// Sequence Publisher(1) - Array
print("--- Sequence Publisher(1) - Array ---")

[1, 2, 3].publisher
    .sink { completion in
        switch completion {
        case .finished:
            print("완료")
        case .failure(let error):
            print("에러 발생")
        }
    } receiveValue: { num in
        print(num)
    }


//Sequnce Publisher(2) - Set
print("--- Sequence Publisher(2) - Set ---")
Set([1, 2, 3, 1]).publisher
    .sink { num in
        print(num)
    }

//Sequnce Publisher(3) - Dictionary
print("--- Sequence Publisher(3) - Dictionary ---")

["A": 1, "B": 2, "C": 3].publisher
    .sink { key, value in
        print("key:", key, "value:", value)
    }


//Just
print("--- Just(1) ---")
Just("안녕하세요")
    .sink { string in
        print(string)
    }

print("--- Just(2) ---")
Just(1)
    .sink { num in
        print(num)
    }

//Empty
print("--- Empty ---")
let emptyPublihser = Empty<Int, Never>()
//let emptyPublihser = Empty<Int, Never>(completeImmediately: true)
//let emptyPublihser = Empty<Int, Never>(completeImmediately: false)

emptyPublihser
    .sink { completion in
        print(completion)
    } receiveValue: { num in
        print(num)
    }


//Fail
enum MyError: Error {
    case ohMyGod
}

let failPublisher = Fail<Int, MyError>(error: MyError.ohMyGod)

//failPublisher
    .sink { completion in
        switch completion {
        case .finished:
            print("Finished")
        case .failure(let error):
            print("Error: \(error)")
        }
    } receiveValue: { num in
        print("Value: \(num)")
    }


//Just, Empty, Fail이 필요한 이유
print("--- Just, Empty, Fail이 필요한 이유 ---")
func fetchSite() -> AnyPublisher<Data, URLError> {
//    guard let url = URL(string: "https://www.naver.com") else {
    guard let url = URL(string: "ht!tps://www.naver.com") else {
//        return Empty<Data, URLError>()
//            .eraseToAnyPublisher()
        return Fail<Data, URLError>(error: URLError(.badURL))
            .eraseToAnyPublisher()
    }
    
    let publisher = URLSession.shared.dataTaskPublisher(for: url)
        .map { (data: Data, response: URLResponse) in
            return data
        }
        .eraseToAnyPublisher()
    return publisher
}

let subscription = fetchSite()
    .sink { completion in
        switch completion {
        case .finished:
            print("완료")
        case .failure(let error):
            print("에러 발생 \(error)")
        }
    } receiveValue: { data in
        print(data)
    }



//Future
print("--- Future ---")
//URLSession.shared.dataTask(with: <#T##URLRequest#>, completionHandler: <#T##(Data?, URLResponse?, (any Error)?) -> Void#>)
//URLSession.shared.dataTaskPublisher(for: <#T##URL#>)

// 기존에 있던 비동기 함수를 Combine형태로 바꾸고 싶을때

var cancellables = Set<AnyCancellable>()

Future<Data, Never> { promise in
    URLSession.shared.dataTask(with: URL(string: "https://naver.com")!) { data, response, error in
        if let data = data {
            promise(.success(data))
        }
    }.resume()
}
.sink { data in
    print("future data:", data)
}
.store(in: &cancellables)
