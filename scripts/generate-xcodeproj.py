import os
import uuid

def gen_id():
    return uuid.uuid4().hex[:24].upper()

def create_project():
    project_dir = "iosApp/iosApp.xcodeproj"
    os.makedirs(project_dir, exist_ok=True)
    
    # Generate unique PBX IDs
    proj_id = gen_id()
    main_group_id = gen_id()
    app_group_id = gen_id()
    fw_group_id = gen_id()
    products_group_id = gen_id()
    
    target_id = gen_id()
    app_product_id = gen_id()
    
    sources_phase_id = gen_id()
    frameworks_phase_id = gen_id()
    embed_frameworks_phase_id = gen_id()
    resources_phase_id = gen_id()
    
    app_swift_id = gen_id()
    app_swift_build_id = gen_id()
    content_view_id = gen_id()
    content_view_build_id = gen_id()
    info_plist_id = gen_id()
    assets_id = gen_id()
    assets_build_id = gen_id()
    
    shared_fw_file_id = gen_id()
    shared_fw_build_id = gen_id()
    shared_fw_embed_id = gen_id()
    
    config_list_proj_id = gen_id()
    config_proj_debug_id = gen_id()
    config_proj_release_id = gen_id()
    
    config_list_target_id = gen_id()
    config_target_debug_id = gen_id()
    config_target_release_id = gen_id()

    pbxproj = f"""// !$*UTF8*$!
{{
	archiveVersion = 1;
	classes = {{
	}};
	objectVersion = 56;
	objects = {{

/* Begin PBXBuildFile section */
		{app_swift_build_id} /* iOSApp.swift in Sources */ = {{isa = PBXBuildFile; fileRef = {app_swift_id} /* iOSApp.swift */; }};
		{content_view_build_id} /* ContentView.swift in Sources */ = {{isa = PBXBuildFile; fileRef = {content_view_id} /* ContentView.swift */; }};
		{assets_build_id} /* Assets.xcassets in Resources */ = {{isa = PBXBuildFile; fileRef = {assets_id} /* Assets.xcassets */; }};
		{shared_fw_build_id} /* shared.xcframework in Frameworks */ = {{isa = PBXBuildFile; fileRef = {shared_fw_file_id} /* shared.xcframework */; }};
		{shared_fw_embed_id} /* shared.xcframework in Embed Frameworks */ = {{isa = PBXBuildFile; fileRef = {shared_fw_file_id} /* shared.xcframework */; settings = {{ATTRIBUTES = (CodeSignOnCopy, RemoveHeadersOnCopy, ); }}; }};
/* End PBXBuildFile section */

/* Begin PBXCopyFilesBuildPhase section */
		{embed_frameworks_phase_id} /* Embed Frameworks */ = {{
			isa = PBXCopyFilesBuildPhase;
			buildActionMask = 2147483647;
			dstPath = "";
			dstSubfolderSpec = 10;
			files = (
				{shared_fw_embed_id} /* shared.xcframework in Embed Frameworks */,
			);
			name = "Embed Frameworks";
			runOnlyForDeploymentPostprocessing = 0;
		}};
/* End PBXCopyFilesBuildPhase section */

/* Begin PBXFileReference section */
		{app_product_id} /* iosApp.app */ = {{isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = iosApp.app; sourceTree = BUILT_PRODUCTS_DIR; }};
		{app_swift_id} /* iOSApp.swift */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = iOSApp.swift; sourceTree = "<group>"; }};
		{content_view_id} /* ContentView.swift */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = ContentView.swift; sourceTree = "<group>"; }};
		{info_plist_id} /* Info.plist */ = {{isa = PBXFileReference; lastKnownFileType = text.plist.xml; path = Info.plist; sourceTree = "<group>"; }};
		{assets_id} /* Assets.xcassets */ = {{isa = PBXFileReference; lastKnownFileType = folder.assetcatalog; path = Assets.xcassets; sourceTree = "<group>"; }};
		{shared_fw_file_id} /* shared.xcframework */ = {{isa = PBXFileReference; lastKnownFileType = wrapper.xcframework; name = shared.xcframework; path = ../shared/build/XCFrameworks/debug/shared.xcframework; sourceTree = "<group>"; }};
/* End PBXFileReference section */

/* Begin PBXFrameworksBuildPhase section */
		{frameworks_phase_id} /* Frameworks */ = {{
			isa = PBXFrameworksBuildPhase;
			buildActionMask = 2147483647;
			files = (
				{shared_fw_build_id} /* shared.framework in Frameworks */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		}};
/* End PBXFrameworksBuildPhase section */

/* Begin PBXGroup section */
		{main_group_id} = {{
			isa = PBXGroup;
			children = (
				{app_group_id} /* iosApp */,
				{fw_group_id} /* Frameworks */,
				{products_group_id} /* Products */,
			);
			sourceTree = "<group>";
		}};
		{app_group_id} /* iosApp */ = {{
			isa = PBXGroup;
			children = (
				{app_swift_id} /* iOSApp.swift */,
				{content_view_id} /* ContentView.swift */,
				{info_plist_id} /* Info.plist */,
				{assets_id} /* Assets.xcassets */,
			);
			path = iosApp;
			sourceTree = "<group>";
		}};
		{fw_group_id} /* Frameworks */ = {{
			isa = PBXGroup;
			children = (
				{shared_fw_file_id} /* shared.xcframework */,
			);
			name = Frameworks;
			sourceTree = "<group>";
		}};
		{products_group_id} /* Products */ = {{
			isa = PBXGroup;
			children = (
				{app_product_id} /* iosApp.app */,
			);
			name = Products;
			sourceTree = "<group>";
		}};
/* End PBXGroup section */

/* Begin PBXNativeTarget section */
		{target_id} /* iosApp */ = {{
			isa = PBXNativeTarget;
			buildConfigurationList = {config_list_target_id} /* Build configuration list for PBXNativeTarget "iosApp" */;
			buildPhases = (
				{sources_phase_id} /* Sources */,
				{frameworks_phase_id} /* Frameworks */,
				{embed_frameworks_phase_id} /* Embed Frameworks */,
				{resources_phase_id} /* Resources */,
			);
			buildRules = (
			);
			dependencies = (
			);
			name = iosApp;
			productName = iosApp;
			productReference = {app_product_id} /* iosApp.app */;
			productType = "com.apple.product-type.application";
		}};
/* End PBXNativeTarget section */

/* Begin PBXProject section */
		{proj_id} /* Project object */ = {{
			isa = PBXProject;
			attributes = {{
				BuildIndependentTargetsInParallel = 1;
				LastUpgradeCheck = 1500;
				TargetAttributes = {{
					{target_id} = {{
						CreatedOnToolsVersion = 15.0;
					}};
				}};
			}};
			buildConfigurationList = {config_list_proj_id} /* Build configuration list for PBXProject "iosApp" */;
			compatibilityVersion = "Xcode 14.0";
			developmentRegion = en;
			hasScannedForEncodings = 0;
			knownRegions = (
				en,
				Base,
			);
			mainGroup = {main_group_id};
			productRefGroup = {products_group_id} /* Products */;
			projectDirPath = "";
			projectRoot = "";
			targets = (
				{target_id} /* iosApp */,
			);
		}};
/* End PBXProject section */

/* Begin PBXResourcesBuildPhase section */
		{resources_phase_id} /* Resources */ = {{
			isa = PBXResourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				{assets_build_id} /* Assets.xcassets in Resources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		}};
/* End PBXResourcesBuildPhase section */

/* Begin PBXSourcesBuildPhase section */
		{sources_phase_id} /* Sources */ = {{
			isa = PBXSourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				{app_swift_build_id} /* iOSApp.swift in Sources */,
				{content_view_build_id} /* ContentView.swift in Sources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		}};
/* End PBXSourcesBuildPhase section */

/* Begin XCBuildConfiguration section */
		{config_proj_debug_id} /* Debug */ = {{
			isa = XCBuildConfiguration;
			buildSettings = {{
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ANALYZER_NONNULL = YES;
				CLANG_CXX_LANGUAGE_STANDARD = "gnu++20";
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = dwarf;
				ENABLE_STRICT_OBJC_MSGSEND = YES;
				ENABLE_TESTABILITY = YES;
				GCC_DYNAMIC_NO_PIC = NO;
				GCC_NO_COMMON_BLOCKS = YES;
				GCC_OPTIMIZATION_LEVEL = 0;
				GCC_PREPROCESSOR_DEFINITIONS = (
					"DEBUG=1",
					"$(inherited)",
				);
				MTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE;
				MTL_FAST_MATH = YES;
				ONLY_ACTIVE_ARCH = YES;
				SDKROOT = iphoneos;
				SWIFT_ACTIVE_COMPILATION_CONDITIONS = DEBUG;
				SWIFT_OPTIMIZATION_LEVEL = "-Onone";
			}};
			name = Debug;
		}};
		{config_proj_release_id} /* Release */ = {{
			isa = XCBuildConfiguration;
			buildSettings = {{
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ANALYZER_NONNULL = YES;
				CLANG_CXX_LANGUAGE_STANDARD = "gnu++20";
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = "dwarf-with-dsym";
				ENABLE_NS_ASSERTIONS = NO;
				ENABLE_STRICT_OBJC_MSGSEND = YES;
				GCC_NO_COMMON_BLOCKS = YES;
				MTL_ENABLE_DEBUG_INFO = NO;
				MTL_FAST_MATH = YES;
				SDKROOT = iphoneos;
				SWIFT_COMPILATION_MODE = wholemodule;
				SWIFT_OPTIMIZATION_LEVEL = "-O";
				VALIDATE_PRODUCT = YES;
			}};
			name = Release;
		}};
		{config_target_debug_id} /* Debug */ = {{
			isa = XCBuildConfiguration;
			buildSettings = {{
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				CODE_SIGN_STYLE = Automatic;
				CURRENT_PROJECT_VERSION = 1;
				DEVELOPMENT_TEAM = XUBRJ499J7;
				ENABLE_PREVIEWS = YES;
				"FRAMEWORK_SEARCH_PATHS[sdk=iphoneos*]" = (
					"$(inherited)",
					"$(SRCROOT)/../shared/build/bin/iosArm64/debugFramework",
				);
				"FRAMEWORK_SEARCH_PATHS[sdk=iphonesimulator*]" = (
					"$(inherited)",
					"$(SRCROOT)/../shared/build/bin/iosSimulatorArm64/debugFramework",
				);
				GENERATE_INFOPLIST_FILE = NO;
				INFOPLIST_FILE = iosApp/Info.plist;
				INFOPLIST_KEY_CFBundleDisplayName = "ADhils Fitness";
				IPHONEOS_DEPLOYMENT_TARGET = 17.0;
				LD_RUNPATH_SEARCH_PATHS = (
					"$(inherited)",
					"@executable_path/Frameworks",
				);
				MARKETING_VERSION = 1.0;
				PRODUCT_BUNDLE_IDENTIFIER = com.dhilip.adhilsfitness;
				PRODUCT_NAME = "$(TARGET_NAME)";
				SUPPORTED_PLATFORMS = "iphoneos iphonesimulator";
				SUPPORTS_MACCATALYST = NO;
				SWIFT_EMIT_LOC_STRINGS = YES;
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = "1,2";
			}};
			name = Debug;
		}};
		{config_target_release_id} /* Release */ = {{
			isa = XCBuildConfiguration;
			buildSettings = {{
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				CODE_SIGN_STYLE = Automatic;
				CURRENT_PROJECT_VERSION = 1;
				DEVELOPMENT_TEAM = XUBRJ499J7;
				ENABLE_PREVIEWS = YES;
				"FRAMEWORK_SEARCH_PATHS[sdk=iphoneos*]" = (
					"$(inherited)",
					"$(SRCROOT)/../shared/build/bin/iosArm64/releaseFramework",
				);
				"FRAMEWORK_SEARCH_PATHS[sdk=iphonesimulator*]" = (
					"$(inherited)",
					"$(SRCROOT)/../shared/build/bin/iosSimulatorArm64/releaseFramework",
				);
				GENERATE_INFOPLIST_FILE = NO;
				INFOPLIST_FILE = iosApp/Info.plist;
				INFOPLIST_KEY_CFBundleDisplayName = "ADhils Fitness";
				IPHONEOS_DEPLOYMENT_TARGET = 17.0;
				LD_RUNPATH_SEARCH_PATHS = (
					"$(inherited)",
					"@executable_path/Frameworks",
				);
				MARKETING_VERSION = 1.0;
				PRODUCT_BUNDLE_IDENTIFIER = com.dhilip.adhilsfitness;
				PRODUCT_NAME = "$(TARGET_NAME)";
				SUPPORTED_PLATFORMS = "iphoneos iphonesimulator";
				SUPPORTS_MACCATALYST = NO;
				SWIFT_EMIT_LOC_STRINGS = YES;
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = "1,2";
			}};
			name = Release;
		}};
/* End XCBuildConfiguration section */

/* Begin XCConfigurationList section */
		{config_list_proj_id} /* Build configuration list for PBXProject "iosApp" */;
		{config_list_proj_id} = {{
			isa = XCConfigurationList;
			buildConfigurations = (
				{config_proj_debug_id} /* Debug */,
				{config_proj_release_id} /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		}};
		{config_list_target_id} /* Build configuration list for PBXNativeTarget "iosApp" */;
		{config_list_target_id} = {{
			isa = XCConfigurationList;
			buildConfigurations = (
				{config_target_debug_id} /* Debug */,
				{config_target_release_id} /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		}};
/* End XCConfigurationList section */
	}};
	rootObject = {proj_id} /* Project object */;
}}
"""
    with open(os.path.join(project_dir, "project.pbxproj"), "w") as f:
        f.write(pbxproj)
    print("Generated iosApp.xcodeproj/project.pbxproj successfully!")

if __name__ == "__main__":
    create_project()
