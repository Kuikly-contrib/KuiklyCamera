Pod::Spec.new do |spec|
  spec.name                     = 'KuiklyCameraIOS'
  spec.version                  = '1.0.0'
  spec.homepage                 = 'https://github.com/Kuikly-contrib/KuiklyCamera'
  spec.source                   = { :git => 'https://github.com/Kuikly-contrib/KuiklyCamera.git', :tag => "#{spec.version}" }
  spec.authors                  = 'Kuikly Team'
  spec.license                  = { :type => 'MIT', :file => 'LICENSE' }
  spec.summary                  = 'KuiklyCamera iOS Native Module - Camera view and module implementation for Kuikly framework'
  spec.description              = <<-DESC
                                  KuiklyCameraIOS provides native iOS implementation for camera functionality in Kuikly framework.
                                  It includes KRCameraView and KRCameraModule for seamless camera integration.
                                  DESC
  
  spec.ios.deployment_target    = '12.0'
  spec.swift_version            = '5.0'
  
  # Source files
  spec.source_files             = 'KuiklyCameraIOS/**/*.{h,m,mm}'
  spec.public_header_files      = 'KuiklyCameraIOS/**/*.h'
  
  # Dependencies
  spec.dependency 'OpenKuiklyIOSRender', '~> 2.7.0'
  
  # Framework settings
  spec.frameworks               = 'UIKit', 'AVFoundation'
  spec.requires_arc             = true
  
  # Compiler flags
  spec.xcconfig = {
    'CLANG_ENABLE_OBJC_ARC' => 'YES'
  }
end
