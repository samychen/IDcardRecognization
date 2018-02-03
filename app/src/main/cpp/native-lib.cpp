#include "common.h"


#define DEFAULT_IDCARD_WIDTH  640
#define DEFAULT_IDCARD_HEIGHT  320

#define DEFAULT_IDNUMBER_WIDTH  240
#define DEFAULT_IDNUMBER_HEIGHT  120

#define  FIX_IDCARD_SIZE Size(DEFAULT_IDCARD_WIDTH,DEFAULT_IDCARD_HEIGHT)
#define  FIX_IDNUMBER_SIZE  Size(DEFAULT_IDNUMBER_WIDTH,DEFAULT_IDNUMBER_HEIGHT)

#define FIX_TEMPLATE_SIZE  Size(150, 26)

extern "C"
JNIEXPORT jobject JNICALL
Java_com_samychen_gracefulwrapper_idcardrecognization_ImageUtils_findIdNumber(JNIEnv *env,
                                                                              jclass type,
                                                                              jobject src,
                                                                              jobject out,
                                                                              jobject tpl,
                                                                              jobject config) {

    //原始图
    Mat img_src;
    //灰度图 需要拿去模版匹配
    Mat img_gray;
    //二值图 进行轮廓检测
    Mat img_threshold;
    //高斯图 进行边界模糊
    Mat img_gaussian;
    //边界图
    Mat img_canny;
    //模版
    Mat img_tpl;
    //获得的身份证图
    Mat img_idCard;
    //获得的身份证号码图
    Mat img_idNumber;
    bitmap2Mat(env, src, img_src);
    bitmap2Mat(env, tpl, img_tpl);
    //灰度化
    cvtColor(img_src, img_gray, COLOR_BGRA2GRAY);
    //二值化
    threshold(img_gray, img_threshold, 100, 255, THRESH_BINARY);
    GaussianBlur(img_threshold,img_gaussian,Size(3,3),0);
    Canny(img_gaussian,img_canny,180,255);
    vector<vector<Point>> contours;
    vector<Vec4i> hierachy;//每个边界轮廓都由上下左右4个坐标组成，所以需要vector数据结构
    //轮廓检测 只检测外轮廓 并压缩水平方向，垂直方向，对角线方向的元素，只保留该方向的终点坐标，比如矩形就是存储四个点
    //findContours(img_threshold, contours, hierachy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
    findContours(img_canny, contours, hierachy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
    int width = img_src.cols >> 1;//图片宽的一半
    int height = img_src.rows >> 1;//图片高的一半
    if (contours.empty()) {
        //可能整张图就是身份证
        img_idCard = img_gray;
    } else {
        Rect roiArea;
        vector<Rect> roiAreas;
        Rect rectMin;
        for (auto points : contours) {
            //根据4个顶点获得区域
            Rect rect = boundingRect(points);
            //身份证轮廓的宽必须大于图片宽的一半
            //高必须大于图片高的一半
            if (rect.width >= width && rect.height >= height) {
                roiArea = rect;
                roiAreas.push_back(rect);
            }
        }
        if (roiAreas.size()>0){
            rectMin = roiAreas.at(0);//找出满足条件的所有轮廓中最小的就是正好身份证的轮廓
            for(int i=0;i<roiAreas.size();i++){
                Rect temp = roiAreas.at(i);
                if (temp.area()<rectMin.area()){
                    rectMin = temp;
                }
            }
        } else{
            rectMin = Rect(0,0,img_gray.cols,img_gray.rows);
        }
        img_idCard = img_gray(rectMin);
        //roiarea有面积
//        if (roiArea.area())
//            img_idCard = img_gray(roiArea);
    }
    resize(img_idCard, img_idCard, FIX_IDCARD_SIZE);//身份证大小640 x 400
    resize(img_tpl, img_tpl, FIX_TEMPLATE_SIZE);
    cvtColor(img_tpl, img_tpl, COLOR_BGRA2GRAY);//使用灰度图进行匹配，彩色图计算量太大
    int cols = img_idCard.cols - img_tpl.cols + 1;
    int rows = img_idCard.rows - img_tpl.rows + 1;
    //创建输出图像，输出图像的宽度 = 被查找图像的宽度 - 模版图像的宽度 + 1
    Mat match(rows, cols, CV_32F);
    //Mat match;
//        TM_SQDIFF 平方差匹配法
//        TM_CCORR 相关匹配法
//        TM_CCOEFF 相关系数匹配法
//        TM_SQDIFF_NORMED
//        TM_CCORR_NORMED
//        TM_CCOEFF_NORMED
    // 对于方法 SQDIFF 和 SQDIFF_NORMED, 越小的数值代表更高的匹配结果. 而对于其他方法, 数值越大匹配越好
    matchTemplate(img_idCard, img_tpl, match, TM_CCORR_NORMED);
    //归一化
    normalize(match, match, 0, 1, NORM_MINMAX, -1);
    Point maxLoc;
    minMaxLoc(match, 0, 0, 0, &maxLoc);
    //计算 [身份证(模版):号码区域]
    //号码区域:
    //x: 身份证(模版)的X+宽
    //y: 身份证(模版)Y
    //w: 全图宽-(身份证(模版)X+身份证(模版)宽) - n(给个大概值)
    //h: 身份证(模版)高
    Rect rect(maxLoc.x + img_tpl.cols, maxLoc.y, img_idCard.cols - (maxLoc.x + img_tpl.cols) - 40,
              img_tpl.rows);
    //拿二值的号码
    resize(img_threshold, img_threshold, FIX_IDCARD_SIZE);
    img_idNumber = img_threshold(rect);
    jobject obj=createBitmap(env,img_idNumber,config);
//    resize(img_idNumber, img_idNumber, FIX_IDNUMBER_SIZE);
//    mat2Bitmap(env, img_idNumber, out);


    img_src.release();
    img_gray.release();
    img_threshold.release();
    img_idCard.release();
    img_idNumber.release();
    img_tpl.release();
    match.release();
    return obj;
}
