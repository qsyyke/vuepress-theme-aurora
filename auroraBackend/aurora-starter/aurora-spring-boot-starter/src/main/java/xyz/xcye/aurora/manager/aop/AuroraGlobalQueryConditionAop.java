package xyz.xcye.aurora.manager.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import xyz.xcye.aurora.util.UserUtils;
import xyz.xcye.core.dto.JwtUserInfo;
import xyz.xcye.core.util.LogUtils;
import xyz.xcye.data.entity.Condition;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author xcye
 * @description 全局的条件拦截，如果用户通过条件进行查询，那么只能查询自己的，超级管理员除外
 * @date 2023-01-02 23:57:53
 */

@Slf4j
@Component
@Aspect
public class AuroraGlobalQueryConditionAop {

    @Around("execution(public * xyz.xcye.*.controller..*.queryList*(..))")
    public Object logProcessRequestTime(ProceedingJoinPoint point) throws Throwable {

        // 获取request
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = null;
        if (requestAttributes != null) {
         request = (HttpServletRequest) requestAttributes.resolveReference(RequestAttributes.REFERENCE_REQUEST);
        }
        if (request != null) {
            // 如果不是从后端发起的请求，从前端发起的话，请求头中会携带aurora_page_web，直接放行
            String auroraPageWeb = request.getHeader("aurora_page_web");
            if (StringUtils.hasLength(auroraPageWeb)) {
                return point.proceed();
            }
        }
        Object[] args = point.getArgs();
        for (Object arg : args) {
            if (arg instanceof Condition) {
                Condition condition = (Condition) arg;
                // 获取登录状态
                JwtUserInfo currentUser = null;
                try {
                    currentUser = UserUtils.getCurrentUser();
                } catch (Exception e) {
                    LogUtils.logExceptionInfo(e);
                }

                if (currentUser != null) {
                    List<String> roleList = currentUser.getRoleList();
                    if (roleList.contains("ROLE_root") || roleList.contains("ROLE_ROOT")) {
                        continue;
                    }

                    Long userUid = currentUser.getUserUid();
                    if (userUid != null) {
                        condition.setOtherUid(userUid);
                    }
                }
            }
        }
        return point.proceed();
    }
}
