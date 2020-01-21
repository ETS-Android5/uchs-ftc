package org.firstinspires.ftc.team11288;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;

import java.util.List;
import java.util.Locale;

import static com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_WITHOUT_ENCODER;
import static com.qualcomm.robotcore.hardware.DcMotor.RunMode.STOP_AND_RESET_ENCODER;

public class UtilMecanum {
    private DcMotor motorRight;
    private DcMotor motorLeft;
    private DcMotor liftMotor;
    private DcMotor armMotor;
    private Telemetry telemetry;
    private DigitalChannel touchSensor;
    private DigitalChannel liftSensor;


    private final double LIFT_MOTOR_POWER = 0.65;
    private final double ARM_MOTOR_POWER = 0.15;
    private final double DRIVE_MOTOR_POWER = 0.75;
    static final double COUNTS_PER_MOTOR_REV = 1250.0; // HD Hex Motor (REV-41-1301) 40:1
    static final double COUNTS_PER_DRIVE_MOTOR_REV = 180; // counts per reevaluation of the motor
    static final double INCREMENT_MOTOR_MOVE = 175.0; // move set amount at a time
    static final double INCREMENT_DRIVE_MOTOR_MOVE = 30.0; // move set amount at a time
    static final double INCHES_PER_ROTATION = 11.137; // inches per rotation of 90mm traction wheel
    static final double DEG_PER_ROTATION = 100.0; // inches per rotation of 90mm traction wheel

    // 2019 Code changes
    private DcMotor motorBackLeft;
    private DcMotor motorBackRight;
    private DcMotor motorFrontLeft;
    private DcMotor motorFrontRight;

    static final int COUNTS_PER_INCH = (int) ((1.4142 * (COUNTS_PER_DRIVE_MOTOR_REV)) / (4.0 * Math.PI)); // for 45deg
                                                                                                          // wheels
    static final int COUNTS_PER_SQUARE = (int) (COUNTS_PER_INCH * 1); // for 45deg wheels
    static final double CENTER_TO_WHEEL_DIST = COUNTS_PER_INCH * 8;//8 inches
    // initialize these in InitExtraSensors if using
    private ColorSensor colorSensor;
    float hsvValues[] = { 0F, 0F, 0F };
    float values[];
    final double SCALE_FACTOR = 255;
    int relativeLayoutId;
    static View relativeLayout;

    ///

    //#region Initialization
    public UtilMecanum(DcMotor frontRightMotor, DcMotor frontLeftMotor, DcMotor backRightMotor, DcMotor backLeftMotor,
                       Telemetry telemetryIn) {

        motorBackLeft = backLeftMotor;
        motorBackRight = backRightMotor;
        motorFrontLeft = frontLeftMotor;
        motorFrontRight = frontRightMotor;

        telemetry = telemetryIn;
        // motorLeft = leftMotor;
        // motorRight = rightMotor;
        // liftMotor = liftMotorIn;
        // armMotor = armMotorIn;
        // telemetry = telemetryIn;
        // touchSensor = touchSensorIn;
        // // liftSensor = liftSensorIn;
    }

    public void InitExtraSensors(HardwareMap hardwareMap) {
        // get a reference to the color sensor.
        colorSensor = hardwareMap.get(ColorSensor.class, "sensor_color_distance");
        // hsvValues is an array that will hold the hue, saturation, and value
        // information.
        hsvValues = new float[] { 0F, 0F, 0F };
        // values is a reference to the hsvValues array.
        float values[] = hsvValues;
        // get a reference to the RelativeLayout so we can change the background
        // color of the Robot Controller app to match the hue detected by the RGB
        // sensor.
        relativeLayoutId = hardwareMap.appContext.getResources().getIdentifier("RelativeLayout", "id",
                hardwareMap.appContext.getPackageName());
        View relativeLayout = ((Activity) hardwareMap.appContext).findViewById(relativeLayoutId);
    }
    //#endregion


    //#region MotorUnilities
    public void setWheelsToEncoderMode() {
        motorBackLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBackRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorFrontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorFrontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        resetEncoderOnMotors();
    }
    public void setWheelsToSpeedMode() {

        motorBackLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorBackRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorFrontLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorFrontRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }
    public void stopWheelsSpeedMode() {

        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
        motorFrontLeft.setPower(0);
        motorFrontRight.setPower(0);
    }
    public void resetEncoderOnMotors(){
        motorFrontRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorFrontLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorBackRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorBackLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
    }
    //#endregion

    //#region Driving
    public void drivebyDistance(double x, double y, double distance, String unit) {// inches
        setWheelsToEncoderMode();
        double r = Math.hypot((-x), (-y));
        double robotAngle = Math.atan2((-y), (-x)) - Math.PI / 4;
        final double v1 = r * Math.cos(robotAngle);
        final double v2 = -r * Math.sin(robotAngle);
        final double v3 = r * Math.sin(robotAngle);
        final double v4 = -r * Math.cos(robotAngle);

        double FrontRight = Range.clip(v2, -1, 1);
        double FrontLeft = Range.clip(v1, -1, 1);
        double BackLeft = Range.clip(v3, -1, 1);
        double BackRight = Range.clip(v4, -1, 1);

        int moveAmount = (int) (distance * COUNTS_PER_INCH);
        // if(unit.equals("inch")) {
        // moveAmount = (int) (distance * COUNTS_PER_INCH);
        // }else
        // if(unit.equals("square")) {
        // moveAmount = (int) (distance * COUNTS_PER_SQUARE);
        // }
        int backLeftTargetPosition = (int) (motorBackLeft.getCurrentPosition() + Math.signum(BackLeft) * moveAmount);
        int backRightTargetPosition = (int) (motorBackRight.getCurrentPosition() + Math.signum(BackRight) * moveAmount);
        int frontLeftTargetPosition = (int) (motorFrontLeft.getCurrentPosition() + Math.signum(FrontLeft) * moveAmount);
        int frontRightTargetPosition = (int) (motorFrontRight.getCurrentPosition()
                + Math.signum(FrontRight) * moveAmount);


        motorBackLeft.setTargetPosition((int) backLeftTargetPosition);
        motorBackRight.setTargetPosition((int) backRightTargetPosition);
        motorFrontLeft.setTargetPosition((int) frontLeftTargetPosition);
        motorFrontRight.setTargetPosition((int) frontRightTargetPosition);

        motorBackLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorBackRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorFrontLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorFrontRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        motorFrontRight.setPower(FrontRight);
        motorFrontLeft.setPower(FrontLeft);
        motorBackLeft.setPower(BackLeft);
        motorBackRight.setPower(BackRight);

        // for those motors that should be busy (power!=0) wait until they are done
        // reaching target position before returning from this function.

        double tolerance = INCREMENT_DRIVE_MOTOR_MOVE;
        while ((((Math.abs(FrontRight)) > 0.01
                && Math.abs(motorFrontRight.getCurrentPosition() - frontRightTargetPosition) > tolerance))
                || (((Math.abs(FrontLeft)) > 0.01
                        && Math.abs(motorFrontLeft.getCurrentPosition() - frontLeftTargetPosition) > tolerance))
                || (((Math.abs(BackLeft)) > 0.01
                        && Math.abs(motorBackLeft.getCurrentPosition() - backLeftTargetPosition) > tolerance))
                || (((Math.abs(BackRight)) > 0.01
                        && Math.abs(motorBackRight.getCurrentPosition() - backRightTargetPosition) > tolerance))) {
            // wait and check again until done running
            telemetry.addData("front right", "=%.2f  %d %b", FrontRight,
                    motorFrontRight.getCurrentPosition() - frontRightTargetPosition,
                    ((Math.ceil(Math.abs(FrontRight)) > 0.0
                            && Math.abs(motorFrontRight.getCurrentPosition() - frontRightTargetPosition) > tolerance)));// ,
                                                                                                                        // frontRightTargetPosition);
            telemetry.addData("front left", "=%.2f %d %b", FrontLeft,
                    motorFrontLeft.getCurrentPosition() - frontLeftTargetPosition,
                    ((Math.ceil(Math.abs(FrontLeft)) > 0.0
                            && Math.abs(motorFrontLeft.getCurrentPosition() - frontLeftTargetPosition) > tolerance)));// ,
                                                                                                                      // frontLeftTargetPosition);
            telemetry.addData("back left", "=%.2f %d %b", BackLeft,
                    motorBackLeft.getCurrentPosition() - backLeftTargetPosition, ((Math.ceil(Math.abs(BackLeft)) > 0.0
                            && Math.abs(motorBackLeft.getCurrentPosition() - backLeftTargetPosition) > tolerance)));// ,
                                                                                                                    // backLeftTargetPosition);
            telemetry.addData("back right", "=%.2f %d %b", BackRight,
                    motorBackRight.getCurrentPosition() - backRightTargetPosition,
                    ((Math.ceil(Math.abs(BackRight)) > 0.0
                            && Math.abs(motorBackRight.getCurrentPosition() - backRightTargetPosition) > tolerance)));
            telemetry.update();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        stopWheelsSpeedMode();

    }
    public void drivebyDistAndRot(double x, double y, double rotation, double distance, String unit) {// inches
        setWheelsToEncoderMode();
        double r = Math.hypot((-x), (-y));
        double robotAngle = Math.atan2((-y), (-x)) - Math.PI / 4;

        double radians = rotation * 3.1415/180;
        double distanceRot = radians * CENTER_TO_WHEEL_DIST;

        final double v1 = r * Math.cos(robotAngle);
        final double v2 = -r * Math.sin(robotAngle);
        final double v3 = r * Math.sin(robotAngle);
        final double v4 = -r * Math.cos(robotAngle);

        double FrontRight = Range.clip(v2, -1, 1);
        double FrontLeft = Range.clip(v1, -1, 1);
        double BackLeft = Range.clip(v3, -1, 1);
        double BackRight = Range.clip(v4, -1, 1);

        telemetry.addData("UtilHolonomic", "FRONTRIGHT=" + FrontRight);
        telemetry.addData("UtilHolonomic", "FRONTLEFT=" + FrontLeft);
        telemetry.addData("UtilHolonomic", "BACKLEFT=" + BackLeft);
        telemetry.addData("UtilHolonomic", "BACKRIGH=" + BackRight);
        telemetry.update();
        int moveAmount = (int) (distance * COUNTS_PER_INCH);
        // if(unit.equals("inch")) {
        // moveAmount = (int) (distance * COUNTS_PER_INCH);
        // }else
        // if(unit.equals("square")) {
        // moveAmount = (int) (distance * COUNTS_PER_SQUARE);
        // }
        int backLeftTargetPosition = (int) (motorBackLeft.getCurrentPosition() + Math.signum(BackLeft) * moveAmount + distanceRot);
        int backRightTargetPosition = (int) (motorBackRight.getCurrentPosition() + Math.signum(BackRight) * moveAmount + distanceRot);
        int frontLeftTargetPosition = (int) (motorFrontLeft.getCurrentPosition() + Math.signum(FrontLeft) * moveAmount + distanceRot);
        int frontRightTargetPosition = (int) (motorFrontRight.getCurrentPosition()
                + Math.signum(FrontRight) * moveAmount + distanceRot);
        


        motorBackLeft.setTargetPosition((int) backLeftTargetPosition);
        motorBackRight.setTargetPosition((int) backRightTargetPosition);
        motorFrontLeft.setTargetPosition((int) frontLeftTargetPosition);
        motorFrontRight.setTargetPosition((int) frontRightTargetPosition);

        motorBackLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorBackRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorFrontLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorFrontRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        int maxTargetPosition = 0;
        int[] tpArray = new int[] {backLeftTargetPosition, backRightTargetPosition, frontLeftTargetPosition, frontRightTargetPosition};
        for(int i = 0; i < 4; i++) {
            if(maxTargetPosition < Math.abs(tpArray[i])) {
                maxTargetPosition = Math.abs(tpArray[i]);
            }
        }

        double mfr = Math.signum(FrontRight)*(frontRightTargetPosition/maxTargetPosition);
        double mfl = Math.signum(FrontLeft)*(frontLeftTargetPosition/maxTargetPosition);
        double mbl = Math.signum(BackLeft)*(backLeftTargetPosition/maxTargetPosition);
        double mbr = Math.signum(BackRight)*(backRightTargetPosition/maxTargetPosition);
        motorFrontRight.setPower(mfr);
        motorFrontLeft.setPower(mfl);
        motorBackLeft.setPower(mbl);
        motorBackRight.setPower(mbr);
        telemetry.addData("[setPower]:", "mfr=%d", mfr);
        telemetry.addData("[setPower]:", "mfl=%d", mfl);
        telemetry.addData("[setPower]:", "mbl=%d", mbl);
        telemetry.addData("[setPower]:", "mbr=%d", mbr);
        // for those motors that should be busy (power!=0) wait until they are done
        // reaching target position before returning from this function.

        double tolerance = INCREMENT_DRIVE_MOTOR_MOVE;
        while ((((Math.abs(FrontRight)) > 0.01
                && Math.abs(motorFrontRight.getCurrentPosition() - frontRightTargetPosition) > tolerance))
                || (((Math.abs(FrontLeft)) > 0.01
                && Math.abs(motorFrontLeft.getCurrentPosition() - frontLeftTargetPosition) > tolerance))
                || (((Math.abs(BackLeft)) > 0.01
                && Math.abs(motorBackLeft.getCurrentPosition() - backLeftTargetPosition) > tolerance))
                || (((Math.abs(BackRight)) > 0.01
                && Math.abs(motorBackRight.getCurrentPosition() - backRightTargetPosition) > tolerance))) {
            // wait and check again until done running
            telemetry.addData("front right", "=%.2f  %d %b", FrontRight,
                    motorFrontRight.getCurrentPosition() - frontRightTargetPosition,
                    ((Math.ceil(Math.abs(FrontRight)) > 0.0
                            && Math.abs(motorFrontRight.getCurrentPosition() - frontRightTargetPosition) > tolerance)));// ,
            // frontRightTargetPosition);
            telemetry.addData("front left", "=%.2f %d %b", FrontLeft,
                    motorFrontLeft.getCurrentPosition() - frontLeftTargetPosition,
                    ((Math.ceil(Math.abs(FrontLeft)) > 0.0
                            && Math.abs(motorFrontLeft.getCurrentPosition() - frontLeftTargetPosition) > tolerance)));// ,
            // frontLeftTargetPosition);
            telemetry.addData("back left", "=%.2f %d %b", BackLeft,
                    motorBackLeft.getCurrentPosition() - backLeftTargetPosition, ((Math.ceil(Math.abs(BackLeft)) > 0.0
                            && Math.abs(motorBackLeft.getCurrentPosition() - backLeftTargetPosition) > tolerance)));// ,
            // backLeftTargetPosition);
            telemetry.addData("back right", "=%.2f %d %b", BackRight,
                    motorBackRight.getCurrentPosition() - backRightTargetPosition,
                    ((Math.ceil(Math.abs(BackRight)) > 0.0
                            && Math.abs(motorBackRight.getCurrentPosition() - backRightTargetPosition) > tolerance)));
            telemetry.update();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        stopWheelsSpeedMode();

    }
    public void drivebySpeed(double x, double y, double rotation) {// inches

        double r = Math.hypot((-x), (-y));
        double robotAngle = Math.atan2((-y), (-x)) - Math.PI / 4;
        double rightX = rotation;
        final double v1 = r * Math.cos(robotAngle) - rightX;
        final double v2 = -r * Math.sin(robotAngle) - rightX;
        final double v3 = r * Math.sin(robotAngle) - rightX;
        final double v4 = -r * Math.cos(robotAngle) - rightX;

        double FrontRight = Range.clip(v2, -1, 1);
        double FrontLeft = Range.clip(v1, -1, 1);
        double BackLeft = Range.clip(v3, -1, 1);
        double BackRight = Range.clip(v4, -1, 1);


        motorBackLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorBackRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorFrontLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorFrontRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        motorFrontRight.setPower(FrontRight);
        motorFrontLeft.setPower(FrontLeft);
        motorBackLeft.setPower(BackLeft);
        motorBackRight.setPower(BackRight);
    }
    @SuppressLint("NewApi")
    public void driveUntilColor(double x, double y, double rotation, double distance, String unit) {// inches
        setWheelsToEncoderMode();
        double r = Math.hypot((-x), (-y));
        double robotAngle = Math.atan2((-y), (-x)) - Math.PI / 4;
        double rightX = rotation;
        final double v1 = r * Math.cos(robotAngle) - rightX;
        final double v2 = -r * Math.sin(robotAngle) - rightX;
        final double v3 = r * Math.sin(robotAngle) - rightX;
        final double v4 = -r * Math.cos(robotAngle) - rightX;

        double FrontRight = Range.clip(v2, -1, 1);
        double FrontLeft = Range.clip(v1, -1, 1);
        double BackLeft = Range.clip(v3, -1, 1);
        double BackRight = Range.clip(v4, -1, 1);

        double moveAmount = distance;
        if (unit.equals("inch")) {
            moveAmount = (int) (distance * COUNTS_PER_INCH);
        } else if (unit.equals("square")) {
            moveAmount = (int) (distance * COUNTS_PER_SQUARE);
        }
        int backLeftTargetPosition = (int) (motorBackLeft.getCurrentPosition() + Math.signum(BackLeft) * moveAmount);
        int backRightTargetPosition = (int) (motorBackRight.getCurrentPosition() + Math.signum(BackRight) * moveAmount);
        int frontLeftTargetPosition = (int) (motorFrontLeft.getCurrentPosition() + Math.signum(FrontLeft) * moveAmount);
        int frontRightTargetPosition = (int) (motorFrontRight.getCurrentPosition()
                + Math.signum(FrontRight) * moveAmount);

        motorBackLeft.setTargetPosition((int) backLeftTargetPosition);
        motorBackRight.setTargetPosition((int) backRightTargetPosition);
        motorFrontLeft.setTargetPosition((int) frontLeftTargetPosition);
        motorFrontRight.setTargetPosition((int) frontRightTargetPosition);

        motorBackLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorBackRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorFrontLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorFrontRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        motorFrontRight.setPower(FrontRight);
        motorFrontLeft.setPower(FrontLeft);
        motorBackLeft.setPower(BackLeft);
        motorBackRight.setPower(BackRight);

        colorSensor.enableLed(true);
        // for those motors that should be busy (power!=0) wait until they are done
        // reaching target position before returning from this function.

        Color.RGBToHSV((int) (colorSensor.red() * SCALE_FACTOR), (int) (colorSensor.green() * SCALE_FACTOR),
                (int) (colorSensor.blue() * SCALE_FACTOR), hsvValues);

        // send the info back to driver station using telemetry function.
        telemetry.addData("Red  ", colorSensor.red());
        telemetry.addData("Green", colorSensor.green());
        telemetry.addData("Blue ", colorSensor.blue());
        telemetry.addData("Hue", hsvValues[0]);
        telemetry.update();
        // change the background color to match the color detected by the RGB sensor.
        // pass a reference to the hue, saturation, and value array as an argument
        // to the HSVToColor method.
        boolean foundBlue = false;
        boolean foundRed = false;

        if (hsvValues[0] > 200 && hsvValues[0] < 250) {
            relativeLayout.setBackgroundColor(Color.BLUE);
            foundBlue = true;
            telemetry.addData("Blue", hsvValues[0]);
            telemetry.update();
        } else {
            // look for the hue in the red range
            if (hsvValues[0] < 10 || hsvValues[0] > 330) {
                relativeLayout.setBackgroundColor(Color.RED);
                foundRed = true;
                telemetry.addData("Blue", hsvValues[0]);
                telemetry.update();
            }
        }

        // updates needed here to drive until foundRed or foundBlue;

        double tolerance = INCREMENT_DRIVE_MOTOR_MOVE;
        while ((((Math.abs(FrontRight)) > 0.01
                && Math.abs(motorFrontRight.getCurrentPosition() - frontRightTargetPosition) > tolerance))
                || (((Math.abs(FrontLeft)) > 0.01
                        && Math.abs(motorFrontLeft.getCurrentPosition() - frontLeftTargetPosition) > tolerance))
                || (((Math.abs(BackLeft)) > 0.01
                        && Math.abs(motorBackLeft.getCurrentPosition() - backLeftTargetPosition) > tolerance))
                || (((Math.abs(BackRight)) > 0.01
                        && Math.abs(motorBackRight.getCurrentPosition() - backRightTargetPosition) > tolerance))
                || (!foundBlue || !foundRed)) {
            // wait and check again until done running
            // telemetry.addData("front right", "=%.2f %d %b", FrontRight,
            // motorFrontRight.getCurrentPosition() -
            // frontRightTargetPosition,((Math.ceil(Math.abs(FrontRight)) > 0.0 &&
            // Math.abs(motorFrontRight.getCurrentPosition() - frontRightTargetPosition) >
            // tolerance)));//, frontRightTargetPosition);
            // telemetry.addData("front left", "=%.2f %d %b", FrontLeft,
            // motorFrontLeft.getCurrentPosition() -
            // frontLeftTargetPosition,((Math.ceil(Math.abs(FrontLeft)) > 0.0 &&
            // Math.abs(motorFrontLeft.getCurrentPosition() - frontLeftTargetPosition) >
            // tolerance)));//, frontLeftTargetPosition);
            // telemetry.addData("back left", "=%.2f %d %b", BackLeft,
            // motorBackLeft.getCurrentPosition() - backLeftTargetPosition,
            // ((Math.ceil(Math.abs(BackLeft)) > 0.0 &&
            // Math.abs(motorBackLeft.getCurrentPosition() - backLeftTargetPosition) >
            // tolerance)));//, backLeftTargetPosition);
            // telemetry.addData("back right", "=%.2f %d %b", BackRight,
            // motorBackRight.getCurrentPosition() - backRightTargetPosition,
            // ((Math.ceil(Math.abs(BackRight)) > 0.0 &&
            // Math.abs(motorBackRight.getCurrentPosition() - backRightTargetPosition) >
            // tolerance)));
            // telemetry.update();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        motorFrontLeft.setPower(0);
        motorBackRight.setPower(0);
        motorFrontRight.setPower(0);
        motorBackLeft.setPower(0);

        colorSensor.enableLed(false);
    }
    //#endregion

    //#region Other Utilities

    public void twoWheelDrive(double leftInput, double rightInput, int mode) {
        double rightDrive = UtilMain.scaleInput(rightInput, mode);
        double leftDrive = UtilMain.scaleInput(-leftInput, mode);

        double finalRight = Range.clip(rightDrive, -1, 1);
        double finalLeft = Range.clip(leftDrive, -1, 1);

        // write the values to the motors
        motorLeft.setPower(finalLeft);
        motorRight.setPower(finalRight);
    }

    void driveFixedDistance(double directionDrive, double inches, boolean isFast) {
        motorRight.setMode(STOP_AND_RESET_ENCODER);
        motorRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorRight.setDirection(DcMotorSimple.Direction.REVERSE);
        motorLeft.setMode(STOP_AND_RESET_ENCODER);
        motorLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        double rotations = inches / INCHES_PER_ROTATION;
        int targetPositionR = (int) (motorRight.getCurrentPosition()
                + (directionDrive * rotations * COUNTS_PER_DRIVE_MOTOR_REV));
        int targetPositionL = (int) (motorLeft.getCurrentPosition()
                + (directionDrive * rotations * COUNTS_PER_DRIVE_MOTOR_REV));
        telemetry.addData("Drive Position R", "= %d  %d  %d", motorRight.getCurrentPosition(),
                motorRight.getTargetPosition(), targetPositionR);
        telemetry.addData("Drive Position L", "= %d  %d  %d", motorLeft.getCurrentPosition(),
                motorLeft.getTargetPosition(), targetPositionL);
        telemetry.update();
        while ((/* touchSensor.getState() == false && */
        Math.abs(motorRight.getCurrentPosition() - (targetPositionR)) > INCREMENT_DRIVE_MOTOR_MOVE)
                || Math.abs(motorLeft.getCurrentPosition() - (targetPositionL)) > INCREMENT_DRIVE_MOTOR_MOVE) {
            motorRight.setTargetPosition(
                    (int) (motorRight.getCurrentPosition() + (int) directionDrive * INCREMENT_DRIVE_MOTOR_MOVE));
            if (isFast) {
                motorRight.setPower(DRIVE_MOTOR_POWER * 2);
            } else {
                motorRight.setPower(DRIVE_MOTOR_POWER);
            }
            motorLeft.setTargetPosition(
                    (int) (motorRight.getCurrentPosition() + (int) directionDrive * INCREMENT_DRIVE_MOTOR_MOVE));
            if (isFast) {
                motorLeft.setPower(DRIVE_MOTOR_POWER * 2);
            } else {
                motorLeft.setPower(DRIVE_MOTOR_POWER);
            }
            telemetry.addData("Drive Position R", "= %d  %d  %d", motorRight.getCurrentPosition(),
                    motorRight.getTargetPosition(), targetPositionR);
            telemetry.addData("Drive Position L", "= %d  %d  %d", motorLeft.getCurrentPosition(),
                    motorLeft.getTargetPosition(), targetPositionL);
            telemetry.update();

        }
        motorRight.setPower(0.0);
        motorRight.setMode(RUN_WITHOUT_ENCODER);
        motorRight.setDirection(DcMotorSimple.Direction.FORWARD);
        motorLeft.setPower(0.0);
        motorLeft.setMode(RUN_WITHOUT_ENCODER);
    }

    void driveAngledDistance(double directionDrive, double inches, boolean isFast) {
        double WEIGHT_R = 1.2;
        motorRight.setMode(STOP_AND_RESET_ENCODER);
        motorRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorRight.setDirection(DcMotorSimple.Direction.REVERSE);
        motorLeft.setMode(STOP_AND_RESET_ENCODER);
        motorLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        double rotations = inches / INCHES_PER_ROTATION;
        int targetPositionR = (int) (motorRight.getCurrentPosition()
                + (directionDrive * WEIGHT_R * rotations * COUNTS_PER_DRIVE_MOTOR_REV));
        int targetPositionL = (int) (motorLeft.getCurrentPosition()
                + (directionDrive * rotations * COUNTS_PER_DRIVE_MOTOR_REV));
        telemetry.addData("Drive Position R", "= %d  %d  %d", motorRight.getCurrentPosition(),
                motorRight.getTargetPosition(), targetPositionR);
        telemetry.addData("Drive Position L", "= %d  %d  %d", motorLeft.getCurrentPosition(),
                motorLeft.getTargetPosition(), targetPositionL);
        telemetry.update();
        while ((/* touchSensor.getState() == false && */
        Math.abs(motorRight.getCurrentPosition() - (targetPositionR)) > INCREMENT_DRIVE_MOTOR_MOVE)
                || Math.abs(motorLeft.getCurrentPosition() - (targetPositionL)) > INCREMENT_DRIVE_MOTOR_MOVE) {
            motorRight.setTargetPosition(
                    (int) (motorRight.getCurrentPosition() + (int) directionDrive * INCREMENT_DRIVE_MOTOR_MOVE));
            if (isFast) {
                motorRight.setPower(WEIGHT_R * DRIVE_MOTOR_POWER * 2);
            } else {
                motorRight.setPower(WEIGHT_R * DRIVE_MOTOR_POWER);
            }
            motorLeft.setTargetPosition(
                    (int) (motorRight.getCurrentPosition() + (int) directionDrive * INCREMENT_DRIVE_MOTOR_MOVE));
            if (isFast) {
                motorLeft.setPower(DRIVE_MOTOR_POWER * 2);
            } else {
                motorLeft.setPower(DRIVE_MOTOR_POWER);
            }
            telemetry.addData("Drive Position R", "= %d  %d  %d", motorRight.getCurrentPosition(),
                    motorRight.getTargetPosition(), targetPositionR);
            telemetry.addData("Drive Position L", "= %d  %d  %d", motorLeft.getCurrentPosition(),
                    motorLeft.getTargetPosition(), targetPositionL);
            telemetry.update();

        }
        motorRight.setPower(0.0);
        motorRight.setMode(RUN_WITHOUT_ENCODER);
        motorRight.setDirection(DcMotorSimple.Direction.FORWARD);
        motorLeft.setPower(0.0);
        motorLeft.setMode(RUN_WITHOUT_ENCODER);
    }

    void driveFixedDegrees(double directionDrive, double degrees) {
        motorRight.setMode(STOP_AND_RESET_ENCODER);
        motorRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorLeft.setMode(STOP_AND_RESET_ENCODER);
        motorLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        double rotations = degrees / DEG_PER_ROTATION;
        int targetPositionR = (int) (motorRight.getCurrentPosition()
                + (directionDrive * rotations * COUNTS_PER_DRIVE_MOTOR_REV));
        int targetPositionL = (int) (motorLeft.getCurrentPosition()
                + (directionDrive * rotations * COUNTS_PER_DRIVE_MOTOR_REV));
        telemetry.addData("Drive Position R", "= %d  %d  %d", motorRight.getCurrentPosition(),
                motorRight.getTargetPosition(), targetPositionR);
        telemetry.addData("Drive Position L", "= %d  %d  %d", motorLeft.getCurrentPosition(),
                motorLeft.getTargetPosition(), targetPositionL);
        telemetry.update();
        while ((/* touchSensor.getState() == false && */
        Math.abs(motorRight.getCurrentPosition() - (targetPositionR)) > INCREMENT_DRIVE_MOTOR_MOVE)
                || Math.abs(motorLeft.getCurrentPosition() - (targetPositionL)) > INCREMENT_DRIVE_MOTOR_MOVE) {
            motorRight.setTargetPosition(
                    (int) (motorRight.getCurrentPosition() + (int) directionDrive * INCREMENT_DRIVE_MOTOR_MOVE));
            motorRight.setPower(DRIVE_MOTOR_POWER);
            motorLeft.setTargetPosition(
                    (int) (motorRight.getCurrentPosition() + (int) directionDrive * INCREMENT_DRIVE_MOTOR_MOVE));
            motorLeft.setPower(DRIVE_MOTOR_POWER);
            telemetry.addData("Drive Position R", "= %d  %d  %d", motorRight.getCurrentPosition(),
                    motorRight.getTargetPosition(), targetPositionR);
            telemetry.addData("Drive Position L", "= %d  %d  %d", motorLeft.getCurrentPosition(),
                    motorLeft.getTargetPosition(), targetPositionL);
            telemetry.update();

        }
        motorRight.setPower(0.0);
        motorRight.setMode(RUN_WITHOUT_ENCODER);
        motorLeft.setPower(0.0);
        motorLeft.setMode(RUN_WITHOUT_ENCODER);
    }

    public void liftDrive(int direction) {
        // if (liftSensor.getState())
        if (direction == 1) {
            liftMotor.setPower(LIFT_MOTOR_POWER);
        } else if (direction == -1) {
            liftMotor.setPower(-LIFT_MOTOR_POWER);
        } else {
            liftMotor.setPower(0);
        }
    }

    void runliftFixedDistance(double directionLift, double rotations) {
        liftMotor.setMode(STOP_AND_RESET_ENCODER);
        liftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        int targetPosition = (int) (liftMotor.getCurrentPosition() + (rotations * COUNTS_PER_MOTOR_REV));
        while ((/* touchSensor.getState() == false && */ Math
                .abs(liftMotor.getCurrentPosition() - (directionLift * targetPosition)) > INCREMENT_MOTOR_MOVE)) {
            liftMotor.setTargetPosition(
                    (int) (liftMotor.getCurrentPosition() + (int) directionLift * INCREMENT_MOTOR_MOVE));
            liftMotor.setPower(directionLift * LIFT_MOTOR_POWER);
            telemetry.addData("Lift Position", "= %d  %d  %d", liftMotor.getCurrentPosition(),
                    liftMotor.getTargetPosition(), targetPosition);
            telemetry.update();

        }
        liftMotor.setPower(0.0);
        liftMotor.setMode(RUN_WITHOUT_ENCODER);
    }

    // Retrieval Routines
    public void servoSet(Servo myServo, double pos) {
        double newPos = Range.clip(pos, -0.5, 0.5);
        myServo.setPosition(newPos);
        telemetry.addData(myServo.getDeviceName(), "= %.2f", newPos);
        telemetry.update();
    }

    public void armDrive(int direction) {
        if (direction == 1) {
            if (touchSensor.getState()) {
                armMotor.setPower(ARM_MOTOR_POWER);
            }
        } else if (direction == -1) {
            armMotor.setPower(-ARM_MOTOR_POWER);
        } else {
            armMotor.setPower(0);
        }
    }

    void runArmFixedDistance(double directionArm, double rotations) {
        armMotor.setMode(STOP_AND_RESET_ENCODER);
        armMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        armMotor.setDirection(DcMotorSimple.Direction.FORWARD);
        int targetPosition = (int) (armMotor.getCurrentPosition() + (directionArm * rotations * COUNTS_PER_MOTOR_REV));
        while ((Math
                .abs(armMotor.getCurrentPosition() - (/* directionArm * */targetPosition)) > INCREMENT_MOTOR_MOVE)) {
            armMotor.setTargetPosition(
                    (int) (armMotor.getCurrentPosition() + (int) directionArm * INCREMENT_MOTOR_MOVE));
            if ((touchSensor.getState() && directionArm == 1) || (directionArm == -1)) {
                armMotor.setPower(/* directionArm * */ARM_MOTOR_POWER);
            }
            telemetry.addData("Arm Position", "= %d  %d  %d", armMotor.getCurrentPosition(),
                    armMotor.getTargetPosition(), targetPosition);
            telemetry.update();

        }
        armMotor.setPower(0.0);
        armMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }


}
