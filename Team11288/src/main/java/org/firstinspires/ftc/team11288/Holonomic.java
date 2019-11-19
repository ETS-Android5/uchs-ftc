// Team11288_Teleop
package org.firstinspires.ftc.team11288;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

import static com.qualcomm.robotcore.hardware.DcMotor.RunMode.STOP_AND_RESET_ENCODER;

/*
 * This file provides Teleop driving for the Team11288 Holonomic drive robot.
 * The code is structured as an Iterative OpMode
 *
 * Assumes claw with arm having shoulder motor, elbow servo and wrist servo - all having 180deg servos
 *
 */

@TeleOp(name="Holonomic Test", group="Teleop")
public class Holonomic extends OpMode{

    /* Declare OpMode members. */
  //wheels
    private DcMotor motorFrontRight;
    private DcMotor motorFrontLeft;
    private DcMotor motorBackRight;
    private DcMotor motorBackLeft;
    private DcMotor motorLift;

    private Util teamUtils;


    //claw and arm
 //  static final double     COUNTS_PER_MOTOR_REV    = 1120 ;    // NeveRest Classic 40 Gearmotor (am-2964a)
    static final double INCREMENT_MOTOR_MOVE = 100; // move about 10 degrees at a time

    private final double ARM_MOTOR_POWER = 0.5;
    private final double DRIVE_MOTOR_POWER = 0.75;
    static final double     COUNTS_PER_MOTOR_REV    = 1250.0; //HD Hex Motor (REV-41-1301) 40:1
    static final double     COUNTS_PER_DRIVE_MOTOR_REV    = 300.0; // counts per reevaluation of the motor
    static final double INCREMENT_DRIVE_MOTOR_MOVE = 30.0; // move set amount at a time

    private DcMotor shoulder; //bottom pivot of the new claw
    private int currentPosition; //used to track shoulder motor current position
    private int targetPosition; //used to track target shoulder position
    private double minPosition; //minimum allowed position of shoulder motor
    private double maxPosition; //maximum allowed positon of shoulder motor

//    private elbow             = null;
//    private Servo wrist       = null;
    private Servo claw        = null;
    private Servo platform    = null;
    private static final double MID_SERVO           =  0.5 ;
    private static final double INIT_ELBOW_SERVO    =  0.0 ;
    private static final double SHOULDER_POWER      =  1.0 ;
    private static final double SHOULDER_UP_POWER   =  0.5 ;
    private static final double SHOULDER_DOWN_POWER = -0.5 ;
    private double          clawOffset  = 0.4 ;                  // Init to closed position
    private final double    CLAW_SPEED  = 0.02 ;                 // sets rate to move servo
    private double          elbowOffset  = 0.0 ;                  // Servo mid position
    private final double    ELBOW_SPEED  = 0.02 ;                  // sets rate to move servo
//    private double          wristOffset  = 0.0 ;                  // Servo mid position
//    private final double    WRIST_SPEED  = 0.02 ;                 // sets rate to move servo
    private static final double INIT_KNOCKINGARM = 0.3;   // Gets the knocking arm out of the way


    //arm for knocking jewel - keep it out of the way in Driver Mode
    private Servo knockingArm = null;
    private static final double SAFE_ARM_POSITION       =  0.0 ;
    //color sensorl
    NormalizedColorSensor colorSensor;

    //TODO touch sensor
    DigitalChannel touchSensor;  // Hardware Device Object


    private int directionArm=1;
    private int rotations=12;
    private int initialPosition;


    /* Code to run ONCE when the driver hits INIT */
        @Override
    public void init() {
        // Initialize the hardware variables.
        // Send telemetry message to signify robot waiting
            telemetry.addData("Say", "Hello Driver");

            //initialize wheels
            motorFrontRight = hardwareMap.dcMotor.get("motor front right");
            motorFrontLeft = hardwareMap.dcMotor.get("motor front left");
            motorBackLeft = hardwareMap.dcMotor.get("motor back left");
            motorBackRight = hardwareMap.dcMotor.get("motor back right");
            motorLift = hardwareMap.dcMotor.get("motor lift");
            claw = hardwareMap.servo.get("claw servo");
            platform = hardwareMap.servo.get("platform servo");

            claw.setPosition(0);
            platform.setPosition(0);
            motorFrontRight.setDirection(DcMotorSimple.Direction.FORWARD);
            motorFrontLeft.setDirection(DcMotorSimple.Direction.FORWARD);
            motorBackRight.setDirection(DcMotorSimple.Direction.FORWARD);
            motorBackLeft.setDirection(DcMotorSimple.Direction.FORWARD);
            motorLift.setDirection(DcMotorSimple.Direction.FORWARD);


            motorLift.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            motorLift.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            motorLift.setMode(STOP_AND_RESET_ENCODER);

            motorLift.setDirection(DcMotorSimple.Direction.FORWARD);
            initialPosition = (int) (motorLift.getCurrentPosition());
            rotations=12;
            directionArm = -1;
            targetPosition = (int) (motorLift.getCurrentPosition() + (directionArm * rotations * COUNTS_PER_MOTOR_REV));

            //utils class initializer
            teamUtils = new Util(motorFrontRight, motorFrontLeft, motorBackRight, motorBackLeft);

    }
    /*
      * Code to run REPEATEDLY after the driver hits INIT, but before they hit PLAY
      */
    @Override
    public void init_loop() {
    }

    /*
     * Code to run ONCE when the driver hits PLAY
     */
    @Override
    public void start() {

    }

    /*
     * Code to run REPEATEDLY after the driver hits PLAY but before they hit STOP
     */


    @Override
    public void loop() {
        double r = Math.hypot(scaleInput(gamepad1.left_stick_x), scaleInput(gamepad1.left_stick_y));
        double robotAngle = Math.atan2(scaleInput(gamepad1.left_stick_y), scaleInput(-gamepad1.left_stick_x)) - Math.PI / 4;
        double rightX = scaleInput(gamepad1.right_stick_x);
        final double v1 = r * Math.cos(robotAngle) - rightX;
        final double v2 = -r * Math.sin(robotAngle) - rightX;
        final double v3 = r * Math.sin(robotAngle) - rightX;
        final double v4 = -r * Math.cos(robotAngle) - rightX;

        double FrontRight = Range.clip(v2, -1, 1);
        double FrontLeft = Range.clip(v1, -1, 1);
        double BackLeft = Range.clip(v3, -1, 1);
        double BackRight = Range.clip(v4, -1, 1);

        // write the values to the motors
        motorFrontRight.setPower(FrontRight);
        motorFrontLeft.setPower(FrontLeft);
        motorBackLeft.setPower(BackLeft);
        motorBackRight.setPower(BackRight);

        //platformArm
        if(gamepad1.a) {
            platform.setPosition(0);
            telemetry.addData("MyActivity", "ServoPosition=0");
            telemetry.update();
        } else if(gamepad1.y) {
            platform.setPosition(1);
            telemetry.addData("MyActivity", "ServoPosition=1");
            telemetry.update();
        }

        //claw
        if(gamepad2.right_bumper){
            claw.setPosition(1);
            telemetry.addData("MyActivity", "ClawPosition=1");
            telemetry.update();
        } if (gamepad2.left_bumper){
            claw.setPosition(0);
            telemetry.addData("MyActivity", "ClawPosition=0");
            telemetry.update();
        }

        ///Code from 2017 - this is how the holonomic drive can be
        //set up to drive a fixed number of inches
        //can test here in teleop and bring into autonomous
        //drive fixed amount
        double distanceToDrive=6.0;  //test parameter
        teamUtils.setWheelsToEncoderMode();
        //driveByDistance - speed in each direction and inches to travel
        //ex: travel 12 inches in positive X direction at quarter speed
        //drivebyDistance(0.25,0.0,0.0,12.0);
        if (gamepad1.left_bumper) {
            distanceToDrive=distanceToDrive+1.0<36?distanceToDrive+1:36;
        }
        if (gamepad1.right_bumper) {
            distanceToDrive=distanceToDrive-1.0>0.0?distanceToDrive-1.0:0.0;
        }
        telemetry.addData("Distance To Drive",  "= %.2f", distanceToDrive);
        telemetry.update();
        if (gamepad1.dpad_up) {
            teamUtils.drivebyDistance(0.0, 0.25, 0.0, distanceToDrive);
        }
        if (gamepad1.dpad_down) {
            teamUtils.drivebyDistance(0.0, -0.25, 0.0, distanceToDrive);
        }
        if (gamepad1.dpad_right) {
            teamUtils.drivebyDistance(0.25, 0.0, 0.0, distanceToDrive);
        }
        if (gamepad1.dpad_left) {
            teamUtils.drivebyDistance(-0.25, 0.0, 0.0, distanceToDrive);
        }
        if (gamepad1.dpad_right) {
            teamUtils.drivebyDistance(0.0, 0.0, 0.25, distanceToDrive);
        }
        if (gamepad1.dpad_left) {
            teamUtils. drivebyDistance(0.0, 0.0, -0.25, distanceToDrive);
        }
        telemetry.update();
        /////




        motorLift.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        if(gamepad2.dpad_up && !gamepad2.x) {
            motorLift.setTargetPosition(Globals.max_claw_limit);
            motorLift.setPower(100);
        }
        motorLift.setPower(0.0);
        if(gamepad2.dpad_down && !gamepad2.x) {
            motorLift.setPower(-0.5);

        }
        if(gamepad2.x && gamepad2.dpad_down){
            Globals.min__claw_limit = motorLift.getCurrentPosition();
        }
        if(gamepad2.x && gamepad2.dpad_up){
            Globals.max_claw_limit = motorLift.getCurrentPosition();
        }


    }

    /*
    * Code to run ONCE after the driver hits STOP
    */
    @Override
    public void stop() {
    }

    /*
     * This method scales the joystick input so for low joystick values, the
     * scaled value is less than linear.  This is to make it easier to drive
     * the robot more precisely at slower speeds.
     */
    double scaleInput(double dVal) {
        //original curve
        //double[] scaleArray = {0.0, 0.05, 0.09, 0.10, 0.12, 0.15, 0.18, 0.24,
          //      0.30, 0.36, 0.4883, 0.50, 0.60, 0.72, 0.85, 1.00, 1.00};

        //1/2y =1/2x^3
        //double[] scaleArray = {0.0, 0.002, 0.002, 0.006, 0.015, 0.03, 0.05, 0.08,
        // 0.125, 0.17, 0.24, 0.3, 0.4, 0.5, 0.67, 0.82, 1.00};
//1/2y = x^3
        double[] scaleArray = {0.0, 0.0, 0.003, 0.01, 0.03, 0.06, 0.1, 0.167,
                0.25, 0.36, 0.43, 0.6499, 0.84, 1.00, 1.00, 1.00, 1.00};

        // get the corresponding index for the scaleInput array.
        int index = Math.abs((int) (dVal * 16.0));
        //index cannot exceed size of array minus 1.
        if (index > 16) {
            index = 16;
        }
        // get value from the array.
        double dScale = 0.0;
        if (dVal < 0) {
            dScale = -scaleArray[index];
        } else {
            dScale = scaleArray[index];
        }
        // return scaled value.
        return dScale;
    }

    public void drive(double lsx, double lsy, double rsx) {
        double r = Math.hypot(scaleInput(lsx), scaleInput(lsy));
        double robotAngle = Math.atan2(scaleInput(lsy), scaleInput(-lsx)) - Math.PI / 4;
        double rightX = scaleInput(rsx);
        final double v1 = r * Math.cos(robotAngle) - rightX;
        final double v2 = -r * Math.sin(robotAngle) - rightX;
        final double v3 = r * Math.sin(robotAngle) - rightX;
        final double v4 = -r * Math.cos(robotAngle) - rightX;

        double FrontRight = Range.clip(v2, -1, 1);
        double FrontLeft = Range.clip(v1, -1, 1);
        double BackLeft = Range.clip(v3, -1, 1);
        double BackRight = Range.clip(v4, -1, 1);

        // write the values to the motors
        motorFrontRight.setPower(FrontRight);
        motorFrontLeft.setPower(FrontLeft);
        motorBackLeft.setPower(BackLeft);
        motorBackRight.setPower(BackRight);
    }
}
