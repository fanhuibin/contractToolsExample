-- Rename legacy numeric point codes to custom ZX codes (idempotent)
-- NOTE: Ensure these codes are unique in your environment before running.

-- 主体/法律引用/财务条款/履行/验收/知产/保密/违约/不可抗力/争议解决/合同解除/合同形式与生效/其他

-- 合同主体
UPDATE review_point SET point_code='ZX-0001' WHERE point_code='3649';
UPDATE review_point SET point_code='ZX-0002' WHERE point_code='3702';

-- 法律引用
UPDATE review_point SET point_code='ZX-0003' WHERE point_code='605';

-- 财务条款
UPDATE review_point SET point_code='ZX-0004' WHERE point_code='517';
UPDATE review_point SET point_code='ZX-0005' WHERE point_code='3521';
UPDATE review_point SET point_code='ZX-0006' WHERE point_code='3549';
UPDATE review_point SET point_code='ZX-0007' WHERE point_code='375';
UPDATE review_point SET point_code='ZX-0008' WHERE point_code='704';
UPDATE review_point SET point_code='ZX-0009' WHERE point_code='3524';
UPDATE review_point SET point_code='ZX-0010' WHERE point_code='378';
UPDATE review_point SET point_code='ZX-0011' WHERE point_code='3523';
UPDATE review_point SET point_code='ZX-0012' WHERE point_code='3445';

-- 履行
UPDATE review_point SET point_code='ZX-0013' WHERE point_code='578';
UPDATE review_point SET point_code='ZX-0014' WHERE point_code='689';

-- 验收
UPDATE review_point SET point_code='ZX-0015' WHERE point_code='369';
UPDATE review_point SET point_code='ZX-0016' WHERE point_code='3388';
UPDATE review_point SET point_code='ZX-0017' WHERE point_code='370';

-- 知识产权
UPDATE review_point SET point_code='ZX-0018' WHERE point_code='3423';
UPDATE review_point SET point_code='ZX-0019' WHERE point_code='674';

-- 保密
UPDATE review_point SET point_code='ZX-0020' WHERE point_code='652';

-- 违约责任
UPDATE review_point SET point_code='ZX-0021' WHERE point_code='651';
UPDATE review_point SET point_code='ZX-0022' WHERE point_code='356';
UPDATE review_point SET point_code='ZX-0023' WHERE point_code='478';
UPDATE review_point SET point_code='ZX-0024' WHERE point_code='567';
UPDATE review_point SET point_code='ZX-0025' WHERE point_code='374';
UPDATE review_point SET point_code='ZX-0026' WHERE point_code='3430';
UPDATE review_point SET point_code='ZX-0027' WHERE point_code='3446';

-- 不可抗力
UPDATE review_point SET point_code='ZX-0028' WHERE point_code='653';

-- 争议解决
UPDATE review_point SET point_code='ZX-0029' WHERE point_code='3566';
UPDATE review_point SET point_code='ZX-0030' WHERE point_code='3578';
UPDATE review_point SET point_code='ZX-0031' WHERE point_code='3570';

-- 合同解除
UPDATE review_point SET point_code='ZX-0032' WHERE point_code='3442';

-- 合同形式与生效
UPDATE review_point SET point_code='ZX-0033' WHERE point_code='3568';
UPDATE review_point SET point_code='ZX-0034' WHERE point_code='3520';
UPDATE review_point SET point_code='ZX-0035' WHERE point_code='3518';
UPDATE review_point SET point_code='ZX-0036' WHERE point_code='3424';

-- 其他
UPDATE review_point SET point_code='ZX-0037' WHERE point_code='3437';
UPDATE review_point SET point_code='ZX-0038' WHERE point_code='3422';


