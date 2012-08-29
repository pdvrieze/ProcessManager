DROP FUNCTION refreshToken;

delimiter //
CREATE DEFINER='webauthadm'@'localhost' FUNCTION refreshToken(tokenparam VARCHAR(45))
RETURNS INT
SQL SECURITY DEFINER
NOT DETERMINISTIC
MODIFIES SQL DATA
BEGIN
  DECLARE mytimestamp INT DEFAULT 0;
  DECLARE myuser VARCHAR(30) DEFAULT "hello";
--  SELECT UNIX_TIMESTAMP() INTO @mytimestamp;
  SET @mytimestamp = UNIX_TIMESTAMP();
  DELETE FROM tokens WHERE (epoch +1800 < @mytimestamp);
  BEGIN
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET @myuser = NULL;
    SELECT user INTO @myuser FROM tokens WHERE token = tokenparam AND IS_NULL(keyid) AND (epoch + 1800) > @mytimestamp;
  END;
  IF IS_NULL(@myuser) THEN
    RETURN 0;
  ELSE
    UPDATE tokens SET epoch=@mytimestamp WHERE token=tokenparam;
    RETURN @mytimestamp + 1800;
  END IF;
END//
delimiter ;