[![](https://jitpack.io/v/liyuzhao/autoupdate.svg)](https://jitpack.io/#liyuzhao/autoupdate)


#### Step 1.
Add it in your root build.gradle at the end of repositories:

```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

#### Step 2.
Add the dependency

```
	dependencies {
	        implementation 'com.github.liyuzhao:autoupdate:v1.0.1'
	}

```