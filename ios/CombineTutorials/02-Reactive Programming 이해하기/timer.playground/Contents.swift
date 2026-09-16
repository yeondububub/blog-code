import Combine
import Foundation

let timerPublisher = Timer.publish(every: 1, on: .main, in: .common)
    //.autoconnect()

let subscriber = timerPublisher
    .map { date in
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss:SSSSS"
        return formatter.string(from: date)
    }
    .sink { date in
        print("date: \(date)")
    }

timerPublisher.connect()
