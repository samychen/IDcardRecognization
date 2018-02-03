#include "common.h"


#define DEFAULT_IDCARD_WIDTH  640
#define DEFAULT_IDCARD_HEIGHT  320

#define DEFAULT_IDNUMBER_WIDTH  240
#define DEFAULT_IDNUMBER_HEIGHT  120

#define  FIX_IDCARD_SIZE Size(DEFAULT_IDCARD_WIDTH,DEFAULT_IDCARD_HEIGHT)
#define  FIX_IDNUMBER_SIZE  Size(DEFAULT_IDNUMBER_WIDTH,DEFAULT_IDNUMBER_HEIGHT)

#define FIX_TEMPLATE_SIZE  Size(150, 26)

extern "C"
JNIEXPORT void JNICALL
Java_com_samychen_gracefulwrapper_idcardrecognization_ImageUtils_findIdNumber(JNIEnv *env,
                                                                              jclass type,
                                                                              jobject src,
                                                                              jobject out,
                                                                              jobject tpl) {

    //原始图
    Mat img_src;
    //灰度图 需要拿去模版匹配
    Mat img_gray;
    //二值图 进行轮廓检测
    Mat img_threshold;
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
    vector<vector<Point>> contours;
    vector<Vec4i> hierachy;
    //轮廓检测 只检测外轮廓 并压缩水平方向，垂直方向，对角线方向的元素，只保留该方向的终点坐标，比如矩形就是存储四个点
    findContours(img_threshold, contours, hierachy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
    int width = img_src.cols >> 1;
    int height = img_src.rows >> 1;
    if (contours.empty()) {
        //可能整张图就是身份证
        img_idCard = img_gray;
    } else {
        Rect roiArea;
        for (auto points : contours) {
            //根据4个顶点获得区域
            Rect rect = boundingRect(points);
            //身份证轮廓的宽必须大于图片宽的一般
            //高必须大于图片高的一般
            if (rect.width >= width && rect.height >= height) {
                roiArea = rect;
            }
        }
        //roiarea有面积
        if (roiArea.area())
            img_idCard = img_gray(roiArea);
    }
    resize(img_idCard, img_idCard, FIX_IDCARD_SIZE);
    resize(img_tpl, img_tpl, FIX_TEMPLATE_SIZE);
    cvtColor(img_tpl, img_tpl, COLOR_BGRA2GRAY);
    int cols = img_idCard.cols - img_tpl.cols + 1;
    int rows = img_idCard.rows - img_tpl.rows + 1;
    //创建输出图像，输出图像的宽度 = 被查找图像的宽度 - 模版图像的宽度 + 1
    Mat match(rows, cols, CV_32F);
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
    resize(img_idNumber, img_idNumber, FIX_IDNUMBER_SIZE);
    mat2Bitmap(env, img_idNumber, out);


    img_src.release();
    img_gray.release();
    img_threshold.release();
    img_idCard.release();
    img_idNumber.release();
    img_tpl.release();
    match.release();

}