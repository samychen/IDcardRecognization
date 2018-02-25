# IDcardRecognization
tesseract-ocr身份证识别

博客地址：[身份证识别](http://samychen.com/2018/02/21/身份证识别/)

&emsp;&emsp;居民二代身份证除了基本信息不同，其他地方都是模板样式，那么我们可以先把敏感信息找到，也就是把身份证号码那一块区域先找到，我们可以把公民身份证那块区别作为匹配模板，找到整张图片的模板所在的区域，而OpenCV刚好提供了模板匹配的方法`matchTemplate( InputArray image, InputArray templ,OutputArray result, int method, InputArray mask = noArray() )`
![身份证样板.png](http://upload-images.jianshu.io/upload_images/4398977-5ac71527080936b6.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

&emsp;&emsp;通过这个方法我们就找到了模板所在的区域，这下子是不是与想法了，我们拿到模板矩阵像素坐标后，是不是可以根据身份证像素坐标的宽度进行一定计算来确定真实身份证号码所在的区域范围了。

接下来我们可以确定真实号码所在区域的结构体范围
* X：模板的左上角像素坐标x加上模板的宽
* Y：模板的y
* W：全图宽-(身份证(模版)X+身份证(模版)宽) - n(给个大概值)
* H：模板的高

&emsp;&emsp;有了以上参数我们就可以把号码所在的区域专门截取出来，是不是已经实现了最重要的功能了。

&emsp;&emsp;当然，拿到号码之后我们还需要对号码去进行识别，这里我们采用tesseract-ocr训练的模型来识别具体号码，关于tesseract-ocr的使用可以自行去谷歌。

&emsp;&emsp;这里还有许多需要优化的地方，身份证原图是彩色图片，考虑到OpenCV计算多通道图片需要耗费性能，在预处理阶段需要先转换为灰度图，之后还需要进行高斯边界模糊处理消除噪声的影响。


演示结果：
![身份证原图.jpg](http://upload-images.jianshu.io/upload_images/4398977-f2370bb06d5b80f1.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

![识别结果.jpg](http://upload-images.jianshu.io/upload_images/4398977-8801d35753e6a224.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

