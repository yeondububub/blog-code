//
//  ViewController.swift
//  01notification_combine
//
//  Created by Junmo Sung on 9/17/26.
//

import UIKit
import Combine

class ViewController: UIViewController {
    
    var cancellables = Set<AnyCancellable>()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
    }
    
    override func viewWillAppear(_ animated: Bool) {
        // 디바이스 방향 변경 알림을 옵저버로 등록
        super.viewWillAppear(animated)
        // NotificationCenter 방식
//        NotificationCenter.default.addObserver(
//            self,
//            selector: #selector(handleOrientationChange),
//            name: UIDevice.orientationDidChangeNotification,
//            object: nilw
//        )
        
        // Combine 방식
        NotificationCenter.default.publisher(for:
            UIDevice.orientationDidChangeNotification)
        .sink { _ in
            self.handleOrientationChange()
        }
        .store(in: &cancellables)
    }

    @objc
    func handleOrientationChange() {
        switch UIDevice.current.orientation {
        case .portrait:
            print("Portrait")
        case .landscapeLeft, .landscapeRight:
            print("Landscape")
        case .portraitUpsideDown:
            print("Upside Down")
        default:
            print("Unknown")
        }
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        //NotificationCenter.default.removeObserver(self)
        cancellables.removeAll()
    }
}


