//
// Created by samychen on 2017/9/3 0003.
//

#ifndef IDCARD_UTILS_H
#define IDCARD_UTILS_H
extern "C" {
void bitmap2Mat(JNIEnv *env, jobject bitmap, Mat &dst,
                jboolean needUnPremultiplyAlpha = false);
void mat2Bitmap(JNIEnv *env, Mat &src, jobject bitmap,
                jboolean needPremultiplyAlpha = false);
jobject createBitmap(JNIEnv *env,Mat srcData,jobject config);
};
#endif //IDCARD_UTILS_H
